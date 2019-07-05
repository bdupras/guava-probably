#!/bin/bash

# see http://benlimmer.com/2013/12/26/automatically-publish-javadoc-to-gh-pages-with-travis-ci/ for details

set -e -u -x

if [ "$TRAVIS_REPO_SLUG" == "bdupras/guava-probably" ] && \
   [ "$TRAVIS_JDK_VERSION" == "oraclejdk8" ] && \
   [ "$TRAVIS_PULL_REQUEST" == "false" ] && \
   [ "$TRAVIS_BRANCH" == "master" ]; then
  echo "Publishing Javadoc ..."

  cd $HOME
  rm -Rf gh-pages
  git clone -q -b gh-pages https://${GH_TOKEN}@github.com/bdupras/guava-probably gh-pages > /dev/null
  cd gh-pages

  git config --global user.email "travis@travis-ci.org"
  git config --global user.name "travis-ci"

  ./updaterelease.sh snapshot

  git push -fq origin gh-pages > /dev/null

  echo "Javadoc published to gh-pages."
fi
