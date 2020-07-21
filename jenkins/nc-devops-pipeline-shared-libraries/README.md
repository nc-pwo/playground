#**Accelerators Shared Library**

---
[![Build Status](http://nc-jenkins-prod-devops.40.113.68.97.nip.io/buildStatus/icon?job=release_nc-devops-pipeline-shared-libraries)](http://nc-jenkins-prod-devops.40.113.68.97.nip.io/job/release_nc-devops-pipeline-shared-libraries/)
<font size="4">
<p>
Shared Library is a collection of resources and functionalties that are accessible for all the steps during a Jenkins build execution.<br>
Another advantage of having Shared Library instead of standard Jenkinsfiles is the fact that functions defined in Shared Library are <i>trusted</i>. <br>
This means that administrator of Jenkins Server do not have to approve each function used in Jenkinsfile. 

In DevOps Accelerators we have implemented ready-to-use pipelines which implement the process of continuous integration and continuous delivery. <br>
Our applications and Docker images are built by Gradle, analysed by Sonarqube, deployed to Artifactory and Openshift.
</p>

For more information about Shared Libraries, please visit: [https://jenkins.io/doc/book/pipeline/shared-libraries/](https://jenkins.io/doc/book/pipeline/shared-libraries/).
</font>
##**How to use it**
---
###**Downloading**
<font size="4">
<p>
You can find the latest release version of shared libraries <a href="https://source.netcompany.com/tfs/Netcompany/NCCGV001/_wiki/wikis/Release%20Notes?wikiVersion=GBmaster&pagePath=%2Fnc_devops_pipeline_shared_libraries%2FRelease%20Notes">here</a>.

If you are interested in particular version (past or future) and what is our approach towards releasing and maintainability, please check-out our [release matrix](https://source.netcompany.com/tfs/Netcompany/NCCGV001/_wiki/wikis/Release%20Notes?wikiVersion=GBmaster&pagePath=%2FRelease%20Matrix) and [this](https://source.netcompany.com/tfs/Netcompany/NCCGV001/_wiki/wikis/NCCGV001.wiki?wikiVersion=GBwikiMaster&pagePath=%2FEnd%252Duser%20Documentation%2FDevOps%20for%20Java%2FSource%20Control%20and%20Workflow%2FRelease%20schedule) document.<br>
</p>

###**Applying configuration**


Depending on your current set-up you may have few options how to apply our solution.<br>
The easiest and simplest is to add it via Jenkins Graphical User Interface in Configure Jenkins Tab.<br>

However, we recommend using Jenkins Configuration as Code Plugin and adding in your configuration file snippet below:

```
 globalLibraries:
    libraries:
      - defaultVersion: "master"
        implicit: true
        includeInChangesets: false
        name: "nc-devops-pipeline-shared-libraries"
        retriever:
          modernSCM:
            scm:
              git:
                credentialsId: *default_credentials
                remote: "ssh://source.netcompany.com:22/tfs/Netcompany/NCCGV001/_git/nc-devops-pipeline-shared-libraries"
                traits:
                  - branchDiscoveryTrait
                  - discoverOtherRefsTrait:
                      ref: "pull/*"
```

Full configuration and working example may be found [here](https://source.netcompany.com/tfs/Netcompany/NCCGV001/_git/nc-devops-jenkins-accelerators?path=%2Fsrc%2Fmain%2Fresources%2Fconfig%2Fjenkins.yaml&version=GBmaster&fullScreen=true).

After configuration of your Jenkins Server, you can start using DevOps Accelerators Shared Library once you import it in your Jenkinsfile:

```
@Library('nc-devops-pipeline-shared-libraries') _
```

Further documentation on how to use our pipelines may be found [here](https://source.netcompany.com/tfs/Netcompany/NCCGV001/_wiki/wikis/NCCGV001.wiki?wikiVersion=GBwikiMaster&pagePath=%2FEnd%252Duser%20Documentation%2FDevOps%20for%20Java%2FJavaBuildPipeline%20in%20your%20project).
</font>
###**Understanding branching model**
<font size="4">
In DevOps Accelerators we are following <a href="https://pl.atlassian.com/git/tutorials/comparing-workflows/gitflow-workflow">git-flow</a> branching model.<br>
What it means is that depending on your needs you may use either:<br>
- <b>master</b> - for latest stable version of Shared-Library,<br>
- <b>develop</b> - for latest version of Shared-Library (may be unstable), contains all new fixes and features,<br>
- <b>tag</b> - according to release schedule we are releasing a specific version of Shared-Library, depending on your needs and configuration you may use those tags in configuration. <b>Using tags is a recommended way of using the library.</b><br>
</font>

##**Contributing**
---
<font size="4">
If you're looking to contribute to our project or provide a patch/pull request, you can find information about our project and procedures <a href="https://source.netcompany.com/tfs/Netcompany/NCCGV001">here</a>.
</font>