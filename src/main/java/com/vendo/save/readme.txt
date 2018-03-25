Unzip save.zip, edit save.default.dat to set your birth month/year and run this command:
javaw -cp Save.jar com.vendo.apps.Save save.default.dat

The main screen lets you change every value except your birth month/year.  Also, the number of 'investment' rows is determined by the number of entries in the .dat file, and for now you can only change it by editing the file.

It doesn't currently write your changes back to the .dat file but you can copy the default file to another name and edit it with a text editor to set defaults specific to your situation:

copy save.default.dat save.yourname.dat
edit save.yourname.dat
javaw -cp Save.jar com.vendo.apps.Save save.yourname.dat

This is the first version, so let me know if you have any problems, or suggestions for improvement (I already have a bunch of ideas, but I'd like to hear others).

-dave
