# Grouping Messages

This is an android app that categorizes your sms into various buckets like logs, ecommerce, personal, notification etc.

Every trained model will be user specific, so each user can categorize their sms according to their needs. 
It requires some learning at the beginning, 
and it will learn with every training data you provide by choosing correct categories for sms if there's a miss somewhere.

The api endpoint for this is- [SMART](https://github.com/reetawwsum/SMART)
This requires a webserver with php and python installed for processing.

You can choose which sender you want to gather data for and whom to ignore,
categories are color coded for easier representation.

Future Roadmap-
* Add preferences for selecting senders, creating buckets
* Add filtering for sms data(_currently only reads unread messages_)
* On phone machine learning making it truly secure and offline
