# helsemelding-attachment-service

The service stores files attached to messages and exposes an API to retrieve them.

## API
The service exposes REST endpoints for storing and retrieving attachments:

#### Store attachments

`POST /attachments/{messageId}`

Accepts a list of attachments with metadata.

Example:
```
[
	{
		"fileName":"attachment.txt",
		"contentType":"text/plain",
		"content":[65,114,98,105,116,114,97]
	},
	{
		"fileName":"attachment2.txt",
		"contentType":"text/plain",
		"content":[77,111,114,101,32,97,114,98,105,116]
	}
]
```

Returns `200 OK` if the attachments were stored successfully.

#### Retrieve attachments

`GET /attachments/{messageId}`

Returns a list of attachments with metadata for the specified message ID.
Returns `200 OK` with the attachments if found, or `404 Not Found` if no attachments exist for the given message ID.

