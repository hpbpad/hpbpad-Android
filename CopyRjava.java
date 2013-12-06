import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class CopyRjava {

    static String GEN_DIR = "gen";
    static String SRC_DIR = "src";
    static String DST_PACKAGE = "com.justsystems.hpb.padalphatest";
    static String PACKAGE = "com.justsystems.hpb.pad";
    static String R_JAVA = "R.java";

    public static void main(String[] args) {
        File gen = new File(GEN_DIR);
        if (!gen.exists()) {
            System.out.println("ERR: not found " + gen.getAbsolutePath());
            return;
        }

        //String cwd = (new File(".")).getAbsoluteFile().getParentFile()
        //        .toString();
        //System.out.println("DEBUG:CWD=" + cwd);
        File target=new File(gen,DST_PACKAGE.replaceAll("\\.", "/"));
        FindFile(target, R_JAVA);
    }

    private static void FindFile(File dir, String file) {
        File[] fs = dir.listFiles();
        for (File f : fs) {
            if (f.isDirectory()) {
                FindFile(f, file);
            } else if (f.isFile()&& f.getName().equals(file)) {
                System.out.println("found R.java = " + f.getAbsolutePath());

                File dst = new File(SRC_DIR + File.separator
                        + PACKAGE.replaceAll("\\.", "/") + File.separator
                        + R_JAVA);
                System.out.println("copy to " + dst.getAbsolutePath());
                try {
                    BufferedReader r = new BufferedReader(
                            new InputStreamReader(new FileInputStream(f),
                                    "UTF-8"));
                    BufferedWriter w = new BufferedWriter(
                            new OutputStreamWriter(new FileOutputStream(dst),
                                    "UTF-8"));
                    String line;
                    while ((line = r.readLine()) != null) {
                        w.write(line.replaceAll("^package.*$", "package "
                                + PACKAGE + ";"));
                        w.newLine();
                    }
                    r.close();
                    w.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                dst.setLastModified(f.lastModified());
            }
        }
    }
}
