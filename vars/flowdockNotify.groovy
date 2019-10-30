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

    // we use the build+XX@flowdock.com addresses for their yay/nay avatars
    def fromAddress = ''

    def colorStatus = ''

    // update subject and set from address based on build status
    switch (buildStatus) {
      case 'SUCCESS':
        def prevResult = script.currentBuild.getPreviousBuild() != null ? script.currentBuild.getPreviousBuild().getResult() : null;
        if (Result.FAILURE.toString().equals(prevResult) || Result.UNSTABLE.toString().equals(prevResult)) {
          subject += ' was fixed'
          fromAddress = 'build+ok@flowdock.com'
          colorStatus = 'green'
          break
        }
        subject += ' was successful'
        fromAddress = 'build+ok@flowdock.com'
        colorStatus = 'green'
        break
      case 'FAILURE':
        subject += ' failed'
        fromAddress = 'build+fail@flowdock.com'
        colorStatus = 'red'
        break
      case 'UNSTABLE':
        subject += ' was unstable'
        fromAddress = 'build+fail@flowdock.com'
        colorStatus = 'yellow'
        break
      case 'ABORTED':
        subject += ' was aborted'
        fromAddress = 'build+fail@flowdock.com'
        colorStatus = 'grey'
        break
      case 'NOT_BUILT':
        subject += ' was not built'
        fromAddress = 'build+fail@flowdock.com'
        colorStatus = 'grey'
        break
      case 'FIXED':
        subject = ' was fixed'
        fromAddress = 'build+ok@flowdock.com'
        colorStatus = 'green'
        break
    }

    def payload

    if (type == 'inbox') {
        // Post is going into the flow as an inbox message

         def content = """<h3>${script.env.JOB_BASE_NAME} ${subject}</h3>
           Build: ${script.currentBuild.displayName}<br />
           Result: <strong>${buildStatus}<br />
           URL: <a href="${script.env.BUILD_URL}">${script.currentBuild.fullDisplayName}</a><br />"""

            payload = JsonOutput.toJson([
                     flow_token: flowToken,
                     event: 'activity',
                     title: script.env.JOB_BASE_NAME,
                     body: content,
                     external_thread_id: script.env.GIT_COMMIT,
                     "status": [
                             "color": colorStatus,
                             "value": buildStatus
                     ],
                     author: [
                         name : script.env.GIT_COMMITTER_NAME,
                         email: fromAddress
                     ]
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
