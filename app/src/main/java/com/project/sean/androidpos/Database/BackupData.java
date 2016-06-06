package com.project.sean.androidpos.Database;


import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

public class BackupData {
    // url for database
    private final String dataPath = "//data//com.project.sean.androidpos//databases//";

    // name of main data
    private final String dataName = AndroidPOSDBHelper.DATABASE_NAME;

    // data main
    private final String data = dataPath + dataName;

    // name of temp data
    private final String dataTempName = AndroidPOSDBHelper.DATABASE_NAME + "_temp";

    // temp data for copy data from sd then copy data temp into main data
    private final String dataTemp = dataPath + dataTempName;

    // folder on sd to backup data
    private final String folderSD = Environment.getExternalStorageDirectory() + "/AndroidPOS";

    private Context context;

    public BackupData(Context context) {
        this.context = context;
    }

    // create folder if it not exist
    private void createFolder() {
        File sd = new File(folderSD);
        if (!sd.exists()) {
            sd.mkdir();
            System.out.println("create folder");
        } else {
            System.out.println("exits");
        }
    }

    /**
     * Copy database to sd card
     * name of file = database name + time when copy
     * When finish, we call onFinishExport method to send notify for activity
     */
    public void exportToSD() {

        String error = null;
        try {

            createFolder();

            File sd = new File(folderSD);

            if (sd.canWrite()) {

                //SimpleDateFormat formatTime = new SimpleDateFormat("yyyy_MM_dd__HH_mm_ss");
                String backupDBPath = dataName;

                File currentDB = new File(Environment.getDataDirectory(), data);
                File backupDB = new File(sd, backupDBPath);

                if (currentDB.exists()) {
                    FileChannel src = new FileInputStream(currentDB).getChannel();
                    FileChannel dst = new FileOutputStream(backupDB).getChannel();
                    dst.transferFrom(src, 0, src.size());
                    src.close();
                    dst.close();
                } else {
                    Toast.makeText(context, "Database does not exist!", Toast.LENGTH_LONG).show();
                    System.out.println("Database does not exist");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            error = "Error backup";
        }
        onBackupListener.onFinishExport(error);
    }

    public void importToSD() {
        String error = null;
        try {
            File sd = new File(folderSD);

            if(sd.exists()) {
                if(sd.canWrite()) {
                    File backupDB = new File(sd, dataName);
                    File currentDB = new File(Environment.getDataDirectory(), data);

                    if(currentDB.exists()) {
                        FileChannel src;
                        try {
                            src = new FileInputStream(backupDB).getChannel();
                            FileChannel dst = new FileOutputStream(currentDB).getChannel();
                            dst.transferFrom(src, 0, src.size());
                            src.close();
                            dst.close();
                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                            error = "Error loading file.";
                        } catch (IOException e) {
                            e.printStackTrace();
                            error = "Error importing file.";
                        }
                    }
                }
            } else {
                Toast.makeText(context, "No folder found.", Toast.LENGTH_LONG).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            error = "Error importing.";
        }

        onBackupListener.onFinishImport(error);
    }

//    /**
//     * import data from file backup on sd card
//     * we must create a temp database for copy file on sd card to it.
//     * Then we copy all row of temp database into main database.
//     * It will keep struct of curren database not change when struct backup database is old
//     *
//     * @param fileNameOnSD name of file database backup on sd card
//     */
//    public void importData(String fileNameOnSD) {
//
//        File sd = new File(folderSD);
//
//        // create temp database
//        SQLiteDatabase dbBackup = context.openOrCreateDatabase(dataTempName,
//                SQLiteDatabase.CREATE_IF_NECESSARY, null);
//
//        String error = null;
//
//        if (sd.canWrite()) {
//
//            File currentDB = new File(Environment.getDataDirectory(), dataTemp);
//            File backupDB = new File(sd, fileNameOnSD);
//
//            if (currentDB.exists()) {
//                FileChannel src;
//                try {
//                    src = new FileInputStream(backupDB).getChannel();
//                    FileChannel dst = new FileOutputStream(currentDB)
//                            .getChannel();
//                    dst.transferFrom(src, 0, src.size());
//                    src.close();
//                    dst.close();
//
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                    error = "Error load file";
//                } catch (IOException e) {
//                    error = "Error import";
//                }
//            }
//        }
//        /**
//         *when copy old database into temp database success
//         * we copy all row of table into main database
//         */
//
//        onBackupListener.onFinishImport(error);
//    }
//


//    /**
//     * show dialog for select backup database before import database
//     * if user select yes, we will export current database
//     * then show dialog to select old database to import
//     * else we only show dialog to select old database to import
//     */
//    public void importFromSD() {
//
//        final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
//        builder.setTitle(R.string.backup_data).setIcon(R.mipmap.ic_launcher)
//                .setMessage(R.string.backup_before_import);
//        builder.setPositiveButton(R.string.no, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                showDialogListFile(folderSD);
//            }
//        });
//        builder.setNegativeButton(R.string.yes, new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                showDialogListFile(folderSD);
//                exportToSD();
//            }
//        });
//        builder.show();
//    }
//
//    /**
//     * show dialog list all backup file on sd card
//     *
//     * @param forderPath folder conatain backup file
//     */
//    private void showDialogListFile(String forderPath) {
//        createFolder();
//
//        File forder = new File(forderPath);
//        File[] listFile = forder.listFiles();
//
//        final String[] listFileName = new String[listFile.length];
//        for (int i = 0, j = listFile.length - 1; i < listFile.length; i++, j--) {
//            listFileName[j] = listFile[i].getName();
//        }
//
//        if (listFileName.length > 0) {
//
//            // get layout for list
//            LayoutInflater inflater = ((FragmentActivity) context).getLayoutInflater();
//            View convertView = (View) inflater.inflate(R.layout.list_backup_file, null);
//
//            final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
//
//            // set view for dialog
//            builder.setView(convertView);
//            builder.setTitle(R.string.select_file).setIcon(R.mipmap.ic_launcher);
//
//            final AlertDialog alert = builder.create();
//
//            ListView lv = (ListView) convertView.findViewById(R.id.lv_backup);
//            ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
//                    android.R.layout.simple_list_item_1, listFileName);
//            lv.setAdapter(adapter);
//            lv.setOnItemClickListener(new OnItemClickListener() {
//                @Override
//                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
//                    alert.dismiss();
//                    importData(listFileName[position]);
//                }
//            });
//            alert.show();
//        } else {
//            final AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.AppCompatAlertDialogStyle);
//            builder.setTitle(R.string.delete).setIcon(R.mipmap.ic_launcher)
//                    .setMessage(R.string.backup_empty);
//            builder.show();
//        }
//    }

    private OnBackupListener onBackupListener;

    public void setOnBackupListener(OnBackupListener onBackupListener) {
        this.onBackupListener = onBackupListener;
    }

    public interface OnBackupListener {
        public void onFinishExport(String error);

        public void onFinishImport(String error);
    }
}