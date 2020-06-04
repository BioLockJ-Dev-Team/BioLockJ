#! /bin/bash

# This script takes a config file and echos either 
# the set of files that would need to be uploaded (if $2==upload) 
# or the set of directories that need to be mounted (if $2==mount)
# to enable BioLockJ running on aws (upload) or in a docker 
# container (mount) to access all files referenced in the config file.

# param 1 - the configFile to read from
# param 2 - one of: upload or mount
main(){
	cd $(dirname "$1")
	primaryConfig=$(basename $1)
	dirList=($(dirname $(to_abs_path $primaryConfig)))
	dockerCopyPath=$(get_host_file $primaryConfig) && primaryConfig=$dockerCopyPath
	
	check_args $@
	
	propNames=()
	propVals=()
	
	read_properties $primaryConfig # populates propNames and propVals

	allProps=()
	split_list_props # populates allProps

	fileList=(primaryConfig)

	if [ $2 == upload ]; then
		find_files # populates dirList and fileList
		echo ${fileList[@]}
	elif [ $2 == mount ]; then
		find_files # populates dirList and fileList
		echo ${dirList[@]}
	elif [ $2 == get ] && [ $# -eq 3 ]; then
		propVal=$(index=$( index_of $3 ${propNames[@]} ) && echo ${propVals[${index}]})
		if [ $propVal == "," ]; then #if propVal was originally empty, a "," might be used as a placeholder
			echo ""; 
		else echo $propVal; fi
	fi
}

. ${BLJ}/script/blj_functions

# param @ - pass along the args
check_args(){ ## TODO use the exit mechanisms offered in blj_functions
	[ ! -f $primaryConfig ] && echo "first argument needs to be a file that I can read." && exit
	[ $# -lt 2 ] || [ $# -gt 3 ] && echo "I need two args, sometimes three" && exit
	[ $2 != "upload" ] && [ $2 != "mount" ] && [ $2 != "get" ] && echo "arg 2 must be one of upload, mount or get" && exit
	[ $2 == "get" ] && [ $# -eq 2 ] && echo "\'get\' requires an additional argument" && exit
	[ $2 != "get" ] && [ $# -gt 2 ] && echo "Only the \'get\' subcommand takes an additional argument" && exit
}

# param 1 - the config file to read
read_properties(){
	#local lines=$(cat $1)
	# first, find any default props
	while read -r line; do
		line="$(echo -e "${line}" | tr -d '[:space:]')"
		if string_starts_with "$line" "#"; then 
			: #ignore it.
		elif ! string_contains "$line" "="; then 
			: #ignore it.
		elif string_starts_with $line "pipeline.defaultProps" || string_starts_with $line "project.defaultProps"; then
			DPROPS=${line##*.defaultProps=}
			DPROPS=${DPROPS//,/" "} # split on ',' because this could be a list
			for DPROP in ${DPROPS[@]}; do
				processedVars=$(eval echo "$DPROP") && DPROP=$processedVars
				dockerCopyPath=$(get_host_file "$DPROP") && DPROP=$dockerCopyPath
				[ ! -f $DPROP ] && echo "Configured default props file, [$DPROP] is not a file." && exit 1
				read_properties "$DPROP"
			done
		fi
	done < $1
	# add props, defaultProps are not special.
	while read -r line; do
		line="$(echo -e "${line}" | tr -d '[:space:]')"
		if [ ${#line} -eq 0 ]; then
			: #ignore it.
		elif string_starts_with "$line" "#"; then 
			: #ignore it.
		elif ! string_contains "$line" "="; then 
			: #ignore it.
		else 
			add_to_props $line
		fi
	done < $1
}

# if in a docker container, copy this file from the host into the container to read it.
# param 1 - host file path
get_host_file(){
	if in_docker_env ; then
		#local newDir="/tmp/vol_$('ls' -l /tmp | wc -l )"
		local newDir="/tmp/vol_0"
		mkdir $newDir
		local parent=$(dirname $1)
		local newPath=$newDir/$(basename "$1")
		local file="/tmpTest/$(basename $1)"
		# cat the file IFF it is a file AND it is not rediculously large (standard.properties is only 6197 bytes)
		#local cmd='[ -f $file ] && [ `stat --printf=\"%s\" $file` -lt 100000 ] && cat $file'
		local cmd='[ -f $file ] && cat $file'
		# copy the file from a peer-docker-container, if that fails (||) exit with error message.
 		docker run --rm -e file=$file --mount type=bind,source=$parent,target=/tmpTest ubuntu "/bin/bash" "-c" "${cmd}" > $newPath || exit_with_message "Could not find file: $1"
		echo $newPath
	else
		return 1
	fi
}

# param 1 - a line from a config file
add_to_props(){
	#get the prop name, %% removes the longest substring from the back
	propName=${1%%=*}
	#get the prop val, # removest the shortest substring from the front
	propVal=${1#*=}
	if [ ${#propVal} -eq 0 ]; then 
		propVal=","; fi # works as a place-holder in propVals, but adds nothing to allProps.
	if ! containsElement $propName ${propNames[@]}; then
		propNames=(${propNames[@]} $propName); fi
	#Add propVal to propVals at index corresponding to propName in propNames.
	local index=$( index_of $propName ${propNames[@]} ) && propVals[${index}]=$propVal
}

split_list_props(){
	for val in ${propVals[@]}; do
		vals=${val//,/" "}
		for v in ${vals[@]}; do
			allProps=(${allProps[@]} $v)
		done
	done
}

find_files(){
	for prop in ${allProps[@]}; do
		processedVars=$(eval echo "$prop") && prop=$processedVars
		if possible_file_path $prop; then
			prop=$(echo ${prop/#..\//`cd ..; pwd`\/})
			[ "$prop" == ".." ] && prop=$(`cd ..; pwd`)
			prop=$(echo ${prop/#.\//`pwd`\/})
			[ "$prop" == "." ] && prop=`pwd`
			if verifyDir "$prop"; then
				add_to_dirList "$prop"
				add_to_fileList "$prop"
			elif verifyFile "$prop"; then
				if string_contains_space $prop ; then echo "WARNING: File paths that contain spaces are often problematic." >&2; fi
				add_to_dirList $(dirname "$prop") #add its parent to dirList()
				add_to_fileList "$prop"
			fi
		fi
	done
}

# if this could be a file (add function to filter out strings that can't be file paths ...)
# param 1 - string that might be a file path
possible_file_path(){
	[ ${#1} -gt 0 ]
}

# param 1 - element to add (IFF it is not already there)
add_to_dirList(){
	if ! containsElement $1 ${dirList[@]}; then
		dirList=(${dirList[@]} $1)
	fi
}

# param 1 - element to add (IFF it is not already there)
add_to_fileList(){
	if ! containsElement $1 ${fileList[@]}; then
		fileList=(${fileList[@]} $1)
	fi
}

main $@