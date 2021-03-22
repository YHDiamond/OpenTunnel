package dev.yhdiamond.opentunnels;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.*;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class OpenTunnels extends JavaPlugin {
    public static String fileName = "ngrok";
    public static String IP = "localhost";
    public static Process auth;
    public static Process update;
    public static Process run;
    @Override
    public void onEnable() {
        saveDefaultConfig();
        String authtoken = getConfig().getString("ngrok-authtoken");
        if (authtoken.equals("placeyourauthtokenhere")) {
            System.out.println("You need to set a valid auth token in the config!");
            System.out.println("You can find instructions on how to do this in our discord (https://discord.gg/tuKjcycH8F) or our spigot page.");
            System.out.println("OpenTunnels will not start :(");
            return;
        }
        File dir = new File("plugins/OpenTunnels");
        if (!dir.exists()) {
            dir.mkdir();
        }
        File file = new File("plugins/OpenTunnels/ngrok.zip");
        if (!file.exists()) {
            try {
                file.createNewFile();
                if (OSUtils.isWindows()) {
                    downloadFile(file, "https://bin.equinox.io/c/4VmDzA7iaHb/ngrok-stable-windows-amd64.zip");
                } else if (OSUtils.isLinux()) {
                    downloadFile(file, "https://bin.equinox.io/c/4VmDzA7iaHb/ngrok-stable-linux-amd64.zip");
                } else if (OSUtils.isMac()) {
                    downloadFile(file, "https://bin.equinox.io/c/4VmDzA7iaHb/ngrok-stable-darwin-amd64.zip");
                } else {
                    System.out.println("We couldn't find your OS properly, OpenTunnel will not start :(");
                    return;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        unzip(file.getPath(), "plugins/OpenTunnels/ngrok");
        try {
            File logfile = new File("tunnellog.log");
            logfile.delete();
            System.out.println("Authenticating tunneling service (ngrok)");
            auth = Runtime.getRuntime().exec("plugins/OpenTunnels/ngrok/" + fileName + " authtoken " + authtoken);
            System.out.println("Updating tunneling service (ngrok)");
            update = Runtime.getRuntime().exec("plugins/OpenTunnels/ngrok/" + fileName + " update");
            System.out.println("Starting tunnel...");
            run = Runtime.getRuntime().exec("plugins/OpenTunnels/ngrok/" + fileName + " tcp " + Bukkit.getPort() + " --log tunnellog.log");
            System.out.println("Tunnel opening in " + getConfig().getInt("delay")/20 + " seconds");

            BukkitRunnable br = new BukkitRunnable() {
                @Override
                public void run() {
                    File ngrokLog = new File("tunnellog.log");
                    try {
                        String logs = "";
                        Scanner scanner = new Scanner(ngrokLog);
                        while (scanner.hasNextLine()) {
                            logs += "\n" + scanner.nextLine();
                        }
                        Pattern pattern = Pattern.compile("url=tcp://(.*)\\s?");
                        Matcher matcher = pattern.matcher(logs);
                        while (matcher.find()) {
                            IP = matcher.group(1);
                        }
                        if (IP.equals("localhost")) {
                            System.out.println("An error occurred in OpenTunnels, please restart the server if you want your friends to connect.");
                            System.out.println("If this error happens multiple times, raise the delay in the config.yml by 100.");
                            System.out.println("You can join our discord for additional support: https://discord.gg/tuKjcycH8F");
                        } else {
                            System.out.println("Your friends can connect with the IP: " + IP);
                            System.out.println("This IP is not permanent! If you restart the server there will be a new IP, look out for it next time.");
                        }
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            };
            File log = new File("tunnellog.log");
            while (true) {
                if (log.exists()) {
                    br.runTaskLater(this, getConfig().getInt("delay"));
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        run.destroyForcibly();
    }

    public static void downloadFile(File file, String urlString) throws IOException {
        Request request = new Request.Builder().url(urlString).build();
        Response response = new OkHttpClient().newCall(request).execute();

        InputStream is = response.body().byteStream();

        BufferedInputStream input = new BufferedInputStream(is);
        OutputStream output = new FileOutputStream(file);

        byte[] data = new byte[1024];

        int count;
        while ((count = input.read(data)) != -1) {
            output.write(data, 0, count);
        }

        output.flush();
        output.close();
        input.close();
    }
    private static void unzip(String zipFilePath, String destDir) {
        File dir = new File(destDir);
        // create output directory if it doesn't exist
        if(!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        //buffer for read and write data to file
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while(ze != null){
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                System.out.println("Unzipping to "+newFile.getAbsolutePath());
                //create directories for sub directories in zip
                new File(newFile.getParent()).mkdirs();
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                //close this ZipEntry
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
