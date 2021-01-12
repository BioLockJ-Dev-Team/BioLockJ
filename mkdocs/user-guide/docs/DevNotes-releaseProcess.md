
# Release process

The release process must be performed by someone with write permission on the main BioLockJ repository.  Since that repository is owned by a GitHub group, anyone with owner permission in the group can perform the steps.

 1. Merge any pull requests that should be included in the release.
 1. Edit the version file to show the release version (ie, remove the _"-dev"_ suffix)
 1. Render all documentation: `cd $BLJ/resources; ant userguide`
 1. Commit these changes, often with with message "version++ to vx.y.z; render docs"
 1. Tag the current master with the tag _"v.x.y.z-rc"_ ("release candidate")
 1. Run release tests ([see details below](#running-release-tests))
 1. Tag the current main branch of the BioLockJ repository with the official release tag.
 1. After saving the results of tests, use the same tag for the sheepdog_testing_suite main branch.
 1. Push the commits and tags to the central main: `git push --tags upstream`
 1. Build the distribution tarball ([see details below](#building-for-deployment))
 1. In GitHub, go to tags, select the new release tag, edit it, and upload the tarball you just created.
 1. Trigger DockerHub builds by pushing to linked github repository ([see details below](#building-for-deployment))
 1. Set new dev version 
 	* Use next _patch_ release (even if the next release is expected to be major).  
 	* After release v1.3.14, set the version file to say "v1.3.15-dev".
	* Commit this with message "Dev continues toward v1.3.15".
 1. **Review**: 
 	 * Use the link to the latest release on the [Getting-Started](https://biolockj.readthedocs.io/en/latest/Getting-Started/) page, and make sure the release appears correct.
 	 * Make sure the user guide link(s) in the top repo [README](https://github.com/BioLockJ-Dev-Team/BioLockJ) both reflect the latest release 
	 	* The view through [github.io](https://biolockj-dev-team.github.io/BioLockJ/) is controlled under the Settings for the BioLockJ repository.
		* The view through [readthedocs](https://biolockj.readthedocs.io/en/latest/) is controlled by the [biolockj](https://readthedocs.org/dashboard/biolockj) project which has multiple admins.
	 * Look for [failed docker builds](#failed-docker-builds).	 
	 	* The auto builds are configured through the [biolockjdevteam](https://hub.docker.com/orgs/biolockjdevteam) organization on DockerHub, which as of late 2020 is a paid account, and has multiple admins, 

### Running release tests

Use the tools in the repository: BioLockJ_Dev_Team/sheepdog_testing_suite.

The tools in this suite will automatically build the BioLockJ program from source, but they will not build the updated docker image.  Many tests run in docker use the `--blj` arg so that the current BioLockJ folder is mapped in, so there is no need to update the image to test a local copy of BioLockJ.  For individual modules, the corresponding docker image probably hasn't changed since the last version, so you can save a bit of time during testing by simply re-tagging the old images with the new version: 
```
$BLJ/resources/docker/docker_build_scripts/retagForLocalTests.sh v1.3.15 v1.3.16
```
Any image whose dockerfile was changed should be built.  And the biolockj_controller should be built (since presumably that has changed since the last version).  To build all images, use the buildDockerImages.sh script with no args. With one arg, any image matching that string will be built.
```
$BLJ/resources/docker/docker_build_scripts/buildDockerImages.sh controller
```

The sheepdog_testing_suite has further instructions for setting up the tests.

 * Use the main branch, and tag it with the same release candidate tag used for the BioLockJ repository.
 * Run each of the /test/run_*_testCollection.sh scripts in the corresponding environment.
 * Save results file under archived_testCollection_results (see existing examples for which files to save)
 * (recommended) Locally save the pipelines for all tests for later reference.  But DO NOT commit these in either repository.
 * If tests fail (that previously passed), reconsider the release.  Make and commit quick fixes if that is feasible.
 * Assuming tests pass, proceed with release process.


### Building for deployment

Best practice for packaging the official release is to download a fresh copy of the official repo, and build within a docker image.  The fresh clone ensures that git-ignored files that are in the local repo copy are incorporated in the official deployment.  Using the docker image promotes consistency, and reduces the chances of invisible dependencies. (Not to mention its downright convenient!)
```
git clone https://github.com/BioLockJ-Dev-Team/BioLockJ.git
cd BioLockJ
docker run --rm -v $PWD:/biolockj biolockjdevteam/build_and_deploy
```

If needed, the `git clone` command could be replaced with `wget https://github.com/BioLockJ-Dev-Team/BioLockJ/archive/main.zip`, or any other download command.

### Triggering docker builds

BioLockJ docker images, most importantly biolockj_controller, are hosted on docker hub under the organization "biolockjdevteam".
The images for modules in that are packaged with the main program, and the image for the BioLockJ program itself, are set up to build on docker hub infrastructure automatically.  For the modules, this typically creates an identical image, and gives it a new tag matching the current release version.  This automated build is triggered  when a tag matching our version format (ie v1.2.3) is pushed to the linked github repository.  As of this writting, dockerhub and github have a nice integration, but it does not allow for linking to a repository owned by an organization (like our BioLockJ repository is owned by the biolockj_dev_team organization).  So we have a separate fork of the repository that exists solely to trigger builds on dockerhub.  The bot user is "biolockjBuilder".  In order to push to this repo, you will need permission.  Any new user who will do the release process will need to be added as a collaborator to that repository.

(first time only) Set up the biolockjBuilder fork as a remote for you BioLockJ git repository:
```bash
git remote add DockerBuilder https://github.com/biolockjBuilder/BioLockJ.git
```

Push the release tag to this repository.
```
git push DockerBuilder --tags
```

Within a few minutes there should be builds scheduled on DockerHub for the auto-build repositories.
They may take some time to actually build.  After a few hours, check the repositories to see that new builds exist and that no builds failed.

### Failed docker builds

Sometimes there are random failures (maybe a website was down temporarily) and you will need to build the image locally and push it with the desired tag.  If the build fails for the biolockj_controller image, that is a big problem and you need to figure out why.  If the build fails for one of the modules, that usually means that a url in the dockerfile needs to be updated.  In some cases, some dependency is no longer available (no longer hosted). In that case, pull the previous version of the image, retag it with the current tag and push to dockerhub.  Make in issue to resolve the problem before the next release.  If the dockerfile can be updated to create a functional image to run the module, great, do that.  If that is not possible, then the most recent image is _the_ image, and the module's docker tag method should no longer use the current biolockj version, but should instead by hard-coded to the most recent version.  Turn off auto-builds for that image.  This is probably a red-flag that the software is no longer supported, and the module will (eventually) need to be replaced.

