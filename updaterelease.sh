#!/bin/bash

#***************************************************************************
#
# Main script for updating release API docs. Can be used to
# either update a specific release version or the current snapshot release.
#
# Usage examples:
#
#   ./updaterelease.sh snapshot
#   ./updaterelease.sh 1.0
#   ./updaterelease.sh 1.0-rc1
#
# All of these update the Javadoc located at _releases/<release>/api/docs ,
# creating those
# directories if this is a new release version. If <release> is 'snapshot',
# Javadoc is derived from the 'master' branch. Otherwise, it is
# derived from the git tag 'v<release>'. In both cases, the actual version
# number is determined by checking out the git branch or tag and getting
# the version number from the pom.xml file via Maven (for non-snapshot
# releases, though, it should always be the same as the <release>
# argument).
#
#***************************************************************************

set -e -u
projectname=guava-probably

# Ensure working dir is the root of the git repo and load util functions.
cd $(dirname $0)
source util/util.sh

ensure_no_uncommitted_changes

# Ensure valid args from user and get the basic variables we need.
if [[ ! $# -eq 1 ]]; then
  echo "Usage: $0 <release>" >&2
  exit 1
fi
release=$1
releaseref=$(git_ref $release)
initialref=$(current_git_ref)

# Create temp directories and files.
tempdir=$(mktemp -d -t ${projectname}-$release-temp.XXX)
logfile=$(mktemp -t ${projectname}-$release-temp-log.XXX)

# Ensure temp files are cleaned up and we're back on the original branch on exit.
function cleanup {
  exitcode=$?
  if [[ "$exitcode" == "0" ]]; then
    rm $logfile
  else
    # Put a newline in case we're in the middle of a "Do something... Done." line
    echo ""
    echo "Update failed: see log at '$logfile' for more details." >&2
    # If we failed while not on the original branch/ref, switch back to it.
    currentref=$(current_git_ref)
    if [[ "$currentref" != "$initialref" ]]; then
      git checkout -q $initialref
    fi
  fi
  rm -fr $tempdir
  exit $exitcode
}
trap cleanup INT TERM EXIT

# Switch to the git ref for the release to do things with the actual repo.
git_checkout_ref $releaseref

# Get the current project version from Maven.
projectversion=$(project_version)

echo "Updating Javadoc for ${projectname} ${projectversion}"

# Copy source files to a temp dir.
cp -r src $tempdir/src

# Compile and generate Javadoc, putting class files in $tempdir/classes and docs in $tempdir/docs.

echo -n "Compiling and generating Javadoc..."
mvn \
    clean \
    compile \
    javadoc:javadoc \
    dependency:build-classpath \
    -Dmdep.outputFile=$tempdir/classpath \
    >> $logfile 2>&1
echo " Done."

mv target/classes $tempdir/classes
mv target/site/apidocs $tempdir/docs

# Cleanup target dir.
rm -fr target

# Switch back to gh-pages.
git_checkout_ref $initialref


# Move generated output to the appropriate final directories.
docsdir=_releases/$release/api/docs
mkdir -p $docsdir && rm -fr $docsdir

echo -n "Moving generated Javadoc to $docsdir..."
mv $tempdir/docs $docsdir
echo " Done."

# Commit
echo -n "Committing changes..."
git add .
git commit -q -m "Generate Javadoc for ${projectname} ${projectversion}"
echo " Done."

# Update version info in _config.yml
if [[ $release == "snapshot" ]]; then
  fieldtoupdate="latest_snapshot"
  version="${projectversion}"
else
  fieldtoupdate="latest_release"
  # The release being updated currently may not be the latest release.
  version=$(latest_release)
fi
sed -i'.bak' -e "s/^$fieldtoupdate:[ ]+.+/$fieldtoupdate: $version/g" _config.yml
if [ -e _config.yml.bak ]; then
  rm _config.yml.bak
fi
if ! git diff --quiet ; then
  echo -n "Updating $fieldtoupdate in _config.yml to $version..."
  git add _config.yml > /dev/null
  git commit -q -m "Update $fieldtoupdate version to $version"
  echo " Done."
fi

echo "Update succeeded."
