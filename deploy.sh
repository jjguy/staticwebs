#!/bin/bash
s3cmd sync --exclude '.DS_Store' --exclude '.git/*' --exclude '.gitignore' --acl-public $1 s3://$2
