#!/usr/bin/env bash
mkdir -p $1
indexFile=$1/index.html
folderName=${PWD##*/}
echo "<html>" >$indexFile
for entry in *
do
	if [[ -d $entry ]]; then
	    echo $1/$entry
	    echo "<a href='$folderName/$entry' type='folder' >$entry</a><br/>" >>$indexFile
	elif [[ -f $entry ]]; then
	    filename=$(basename "$entry")
	    extension="${filename##*.}"
	    filename="${filename%.*}"
	    echo $1/$filename.html
	    echo "<html><a href='$entry' type='file' >$entry</a></html>" >$1/$filename.html
	    echo "<a href='$folderName/$entry'  type='file' >$entry</a><br/>" >>$indexFile
	else
	    echo "$entry is not valid"
	    exit 1
	fi
done
echo "</html>" >>$indexFile

