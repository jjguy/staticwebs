#!/usr/bin/bash
if [ -z "$1" ]
  then
    echo "Usage: deploy.sh [dir] [bucket]
fi

s3cmd sync --exclude '.DS_Store' --exclude '.git/*' --exclude '.gitignore' --acl-public $1 s3://$2
