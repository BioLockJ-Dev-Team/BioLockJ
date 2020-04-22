# Email
Add to module run order:                    
`#BioModule biolockj.module.report.Email`

## Description 
Send an email containing the pipeline summary when the pipeline either completes or fails.

## Properties 
*Properties are the `name=value` pairs in the [configuration](../../../Configuration#properties) file.*                   

### Email properties: 
| Property| Description |
| :--- | :--- |
| *mail.encryptedPassword* | _string_ <br>The Base 64 encrypted password is stored in the Config file using this property.<br>*default:*  7GYvu1m+Yv1Gk7Cd9BLaznJ/jq33g0q1 |
| *mail.from* | _string_ <br>Admin email address used to send user pipeline notifications<br>*default:*  biolockj@gmail.com |
| *mail.smtp.auth* | _string_ <br><br>*default:*  Y |
| *mail.smtp.host* | _string_ <br>javax.mail.Session SMTP host<br>*default:*  smtp.gmail.com |
| *mail.smtp.port* | _integer_ <br><br>*default:*  587 |
| *mail.smtp.starttls.enable* | _boolean_ <br><br>*default:*  Y |
| *mail.to* | _string_ <br><br>*default:*  *null* |

### General properties applicable to this module: 
*none*

## Details 
*none*

## Adds modules 
**pre-requisite modules**                    
*none found*                   
**post-requisite modules**                    
*none found*                   

## Docker 
If running in docker, this module will run in a docker container from this image:<br>
```
biolockjdevteam/biolockj_controller:v1.3.2
```
This can be modified using the following properties:<br>
`Email.imgOwner`<br>
`Email.imageName`<br>
`Email.imageTag`<br>

## Citation 
Module developed by Mike Sioda.                   
BioLockJ v1.3.2-dev

