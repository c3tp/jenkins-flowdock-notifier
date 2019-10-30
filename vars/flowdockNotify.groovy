// Based on:
// https://github.com/jenkinsci/flowdock-plugin/issues/24#issuecomment-271784565

import groovy.json.JsonOutput
import java.net.URLEncoder
import hudson.model.Result

def call(script, flowToken, tags = '') {

    tags = tags.replaceAll("\\s","")

    def flowdockURL = "https://api.flowdock.com/messages"

    // build status of null means successful
    def buildStatus =  script.currentBuild.result ? script.currentBuild.result : 'SUCCESS'

    // create subject
    def subject = "${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName.replaceAll("#", "")}"

    // we use the build+XX@flowdock.com addresses for their yay/nay avatars
    def fromAddress = ''

    // update subject and set from address based on build status
    switch (buildStatus) {
      case 'SUCCESS':
        def prevResult = script.currentBuild.getPreviousBuild() != null ? script.currentBuild.getPreviousBuild().getResult() : null;
        if (Result.FAILURE.toString().equals(prevResult) || Result.UNSTABLE.toString().equals(prevResult)) {
          subject += ' was fixed'
          fromAddress = 'build+ok@flowdock.com'
          break
        }
        subject += ' was successful'
        fromAddress = 'build+ok@flowdock.com'
        break
      case 'FAILURE':
        subject += ' failed'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'UNSTABLE':
        subject += ' was unstable'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'ABORTED':
        subject += ' was aborted'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'NOT_BUILT':
        subject += ' was not built'
        fromAddress = 'build+fail@flowdock.com'
        break
      case 'FIXED':
        subject = ' was fixed'
        fromAddress = 'build+ok@flowdock.com'
        break
    }

    // build message
    def content = """${script.currentBuild.fullDisplayName}
      Result: ${buildStatus}
      URL: ${script.env.BUILD_URL}
      Author: ${script.env.GIT_COMMITTER_NAME}
      Commit: ${script.env.GIT_COMMIT}"""

    // build payload
    def payload = JsonOutput.toJson([
                flow_token: flowToken,
                event: 'message',
                content: content,
                tags:tags
                ])

    // craft and send the request
    def post = new URL(flowdockURL).openConnection();
    post.setRequestMethod("POST");
    post.setDoOutput(true);
    post.setRequestProperty("Content-Type", "application/json")
    post.getOutputStream().write(payload.getBytes("UTF-8"));
    def postRC = post.getResponseCode();

    println("Response received from Flowdock API: " + postRC);
}
