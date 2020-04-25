- Test before release
Test DBs max length for strings - long paths could cause issues.
Real topos / places
Perf test - load huge data set and check performance
Time dB queries

- Do after release
Fix flipper
Firebase DB backups - https://firebase.google.com/docs/firestore/manage-data/export-import
Storage backups?
Tests...
Report topo image
Image scaling, accommodate panoramas. Min / max size for topo image view. (fit to size)
License t&c (Web)
Analytics
Add 'ungraded' & 'project' grades which are the default
Edit/delete things that you own
Add routes to existing topo
Allow marking start/end holds, in / out holds
Filter routes by grade and type
Cache images on phone up to configurable limit
Submission validation of fields to show nice error instead of just disabling button
Allow uploader to edit uploads?
Grade voting
Place hold location on topo
Full screen Topo view
DWS grade / climb  type
Add route button turn into 3 buttons, for each climb type?
Add extra info:
    - Parking spots
    - Approach notes
    - Rock type
    - Route height
Split submission into route paths and info


Nice to haves:
- https://developer.android.com/kotlin/ktx#core-packages
- Clean architecture? RxJava? Single responsibility?
https://proandroiddev.com/offline-apps-its-easier-than-you-think-9ff97701a73f

### GitLab
`brew install gitlab-runner`

To have launchd start gitlab-runner now and restart at login: <br />
`brew services start gitlab-runner`

Or, if you don't want/need a background service you can just run:<br />
`gitlab-runner start`

Changing a git remote: (https://help.github.com/articles/changing-a-remote-s-url/)<br />
`git remote set-url origin {url of new repo}`

Adding a git remote: (https://help.github.com/articles/adding-a-remote/)<br />
`git remote add origin {url of new repo}`

Verify a git remote:<br />
`git remote -v`


##### Data Structure:
```
- Crags (Location)
    - Sectors (Location)
        - Topos (Topo)
            - Routes (Route)
```

Useful Links:
Spock testing Kotlin Android
https://dzone.com/articles/testing-kotlin-with-spock-part-1-object
https://blog.andresteingress.com/2014/07/22/spock-junit-rules
https://stackoverflow.com/questions/48391716/spock-with-mockito-testing-kotlin-classes
https://proandroiddev.com/improve-your-tests-with-kotlin-in-android-pt-1-6d0b04017e80
https://github.com/dpreussler/kotlin-testrunner


Privacy Policy:
https://app.termly.io/document/privacy-policy/9a5525bf-d062-48c8-8517-b1f6c9b6d4ae

https://security.stackexchange.com/questions/3779/how-can-i-export-my-private-key-from-a-java-keytool-keystore
keytool -importkeystore -srckeystore release.jks -destkeystore release.p12 -deststoretype PKCS12 -srcalias wcr_release -deststorepass "PASSWORD HERE" -destkeypass "PASSWORD HERE"
openssl pkcs12 -in release.p12  -nodes -nocerts -out key.pem
