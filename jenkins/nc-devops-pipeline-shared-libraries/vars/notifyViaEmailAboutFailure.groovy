def call(List<String> mailRecipientsList) {
    echo 'Sending emails to given recipients...'

    emailext(to: mailRecipientsList ? mailRecipientsList.join(";") : '',
            subject: '$DEFAULT_SUBJECT',
            body: '$DEFAULT_CONTENT',
            recipientProviders: [culprits()],
            postsendScript: '$DEFAULT_POSTSEND_SCRIPT',
            presendScript: '$DEFAULT_PRESEND_SCRIPT')
}