
Minimalistic Java Utility to remove older files based on available space left on disk
-----------------------------------------------------------------

I use it to rollover videos and images uploaded from surveillance cameras to hard drive before it run out of space   

**Config Parameters**


java -jar /home/pi/files-shrink/files-shrink.jar  */home/pi/mnt/usb1/ftp/files/cameras* *20*

* **1st parameter**: */home/pi/mnt/usb1/ftp/files/cameras* - target directory
* **2nd parameter**: *20* - size of available space on disk in GB to trigger old files deletion 

**Usage Example**

    Unix cron job to remove obsolete files every day at midnight and log output to files-shrink.log


    0 * * * * /home/pi/files-shrink/run.sh  >> /home/pi/files-shrink/files-shrink.log 2>&1



**Source at GitHub**

https://github.com/solovyevk/files-shrink.git

