# zotero-bb

A script I use to randomly select a paper from my "To read" collection.

## Credentials

The script expects the following environment variables to be set:

```
ZOTERO_API_KEY
ZOTERO_USER_ID
```

### Getting the credentials

The user id is a numerical id, not your Zotero login name.

You can find your user id and create an api key at <https://www.zotero.org/settings/keys>.

### Setting the credentials

I set these in my ~/.localrc file:

```
export ZOTERO_API_KEY=...
export ZOTERO_USER_ID=...
```