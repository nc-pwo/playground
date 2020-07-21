# DMS-SHARED-PIPELINE-LIBRARIES #

This project contains the code used by UFSTDMS Jenkins instances. It uses the nc-devops-shared-pipeline-libraries as its dependency.

## Dependencies ##

This project is dependent on Jenkins Pipeline and Jenkins = 2.190.1, though some earlier versions may work, they have not been tested.

## Installation ##

The shared libraries may be accessed one of three ways:

1) Add this repo to 'Global Pipeline Libraries' in the Jenkins UI.

2) Include a libraries block in declarative pipeline syntax.

3) Include this library in an @Library statement in a Pipeline script.

### Global Pipeline Libraries ###

See [this article](https://jenkins.io/doc/book/pipeline/shared-libraries/#global-shared-libraries)
about adding shared libraries via the Jenkins UI.

### Within Pipeline Script ###

The declarative way to include groovy files in vars/ in other libraries is the following.

```
    library('nc-devops-shared-pipeline-libraries')
```

If you want to use class from src/, the following line should be added

```$xslt
library('nc-devops-pipeline-shared-libraries').nc.devops.shared.library.utils.CronUtils
```

## Usage

This shared libraries contains javaBitbucketBpprPipeline and javaBitbucketBuilDMSipeline, which is a customized pipeline from nc-devops-shared-pipeline-libraries for ufstital-post projects.

You can read about [build](https://source.netcompany.com/tfs/Netcompany/NCCGV001/_wiki/wikis/NCCGV001.wiki?wikiVersion=GBwikiMaster&pagePath=%2FEnd%252Duser%20Documentation%2FDevOps%20for%20Java%2FJavaBuilDMSipeline%20in%20your%20project) and [bppr](https://source.netcompany.com/tfs/Netcompany/NCCGV001/_wiki/wikis/NCCGV001.wiki?wikiVersion=GBwikiMaster&pagePath=%2FEnd%252Duser%20Documentation%2FDevOps%20for%20Java%2FJavaBpprPipeline%20in%20your%20project) in DevOps Accelerator wiki.
The implementation is different but the idea for bppr and build pipeline is the same.

## Who do I talk to? ##

Technical Team is responsible for this repository. Contact us if you find any issues. 

## Credits and references ##

1. [Jenkins Pipeline](https://jenkins.io/doc/book/pipeline/shared-libraries/)
2. [Jenkins Automation](https://github.com/cfpb/jenkins-automation)