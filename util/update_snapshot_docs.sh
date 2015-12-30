#!/bin/bash

# see http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/ for details

set -e -u

if [ "$TRAVIS_REPO_SLUG" == "duprasville/guava-probably" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk7" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then
  echo "Publishing Javadoc and JDiff..."

  cd $HOME
  git clone -q -b gh-pages https://${GH_TOKEN}@github.com/bdupras/guava-probably gh-pages > /dev/null
  cd gh-pages

  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "travis-ci"

  ./updaterelease.sh snapshot

  git push -fq origin gh-pages > /dev/null

  echo "Javadoc and JDiff published to gh-pages."
fi
