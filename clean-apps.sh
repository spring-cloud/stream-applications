#!/bin/bash
echo "About to delete the following 'apps' folders:"
find . -type d -name apps
echo "----------------"
echo "Enter 'Y' or 'N':"
read shouldDelete
if [ "$shouldDelete" = "Y" ] || [ "$shouldDelete" = "y" ]; then
  echo "Deleting..."
  find . -type d -name apps -prune -exec rm -rf {} \;
  echo "Done"
else
  echo "Not deleting"
fi
