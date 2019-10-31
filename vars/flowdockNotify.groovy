// Based on:
// https://github.com/jenkinsci/flowdock-plugin/issues/24#issuecomment-271784565

import groovy.json.JsonOutput
import java.net.URLEncoder
import hudson.model.Result

def call(script, type, flowToken, tags = '') {

    tags = tags.replaceAll("\\s","")

    def flowdockURL = "https://api.flowdock.com/messages"

    // build status of null means successful
    def buildStatus =  script.currentBuild.result ? script.currentBuild.result : 'SUCCESS'

    // create subject
    def subject = "${script.env.JOB_BASE_NAME} build ${script.currentBuild.displayName.replaceAll("#", "")}"

    def colorStatus = ''

    // update subject and set from address based on build status
    switch (buildStatus) {
      case 'SUCCESS':
        def prevResult = script.currentBuild.getPreviousBuild() != null ? script.currentBuild.getPreviousBuild().getResult() : null;
        if (Result.FAILURE.toString().equals(prevResult) || Result.UNSTABLE.toString().equals(prevResult)) {
          subject += ' was fixed'
          colorStatus = 'green'
          break
        }
        subject += ' was successful'
        colorStatus = 'green'
        break
      case 'FAILURE':
        subject += ' failed'
        colorStatus = 'red'
        break
      case 'UNSTABLE':
        subject += ' was unstable'
        colorStatus = 'yellow'
        break
      case 'ABORTED':
        subject += ' was aborted'
        colorStatus = 'grey'
        break
      case 'NOT_BUILT':
        subject += ' was not built'
        colorStatus = 'grey'
        break
      case 'FIXED':
        subject = ' was fixed'
        colorStatus = 'green'
        break
    }

    def payload

    if (type == 'inbox') {
        // Post is going into the flow as an inbox message

         def content = """${subject}
           Build: ${script.currentBuild.displayName}
           Result: ${buildStatus}
           URL: ${script.env.BUILD_URL}"""

         def statusValues = JsonOutput.toJson([
                 color: colorStatus,
                 value: buildStatus
         ])

         def authorValues = JsonOutput.toJson([
                 name : script.env.GIT_COMMITTER_NAME,
                 email: script.env.GIT_COMMITTER_EMAIL
         ])

         def threadValues = JsonOutput.toJson([
                 status: statusValues,
                 body: content,
                 title: subject

         ])

         payload = JsonOutput.toJson([
                 flow_token: flowToken,
                 event: 'activity',
                 external_thread_id: script.env.GIT_COMMIT,
                 thread: threadValues,
                 title: "",
                 author: authorValues
         ])

    } else {
        // Post is going into flow as a chat message
        def content = """${subject}
            Result: ${buildStatus}
            Build: ${script.currentBuild.displayName}
            URL: ${script.env.BUILD_URL}
            Author: ${script.env.GIT_COMMITTER_NAME}
            Commit: ${script.env.GIT_COMMIT}"""

        // build payload
        payload = JsonOutput.toJson([
                flow_token: flowToken,
                event: 'message',
                content: content,
                tags:tags
        ])
    }

    // craft and send the request
    def post = new URL(flowdockURL).openConnection();
    post.setRequestMethod("POST");
    post.setDoOutput(true);
    post.setRequestProperty("Content-Type", "application/json")
    post.getOutputStream().write(payload.getBytes("UTF-8"));
    def postRC = post.getResponseCode();

    println("Response received from Flowdock API: " + postRC);
}
