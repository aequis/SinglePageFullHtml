package com.markfront.spfh;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.tika.Tika;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.vdurmont.emoji.EmojiParser;

public class Utils {
    public static boolean mergeIntoFatHtml(String page_url,
                                           String in_html_file,
                                           String out_html_file,
                                           String curr_dir) {
        boolean success = false;
        try {
            System.out.println("page_url: " + page_url);

            File page_file = new File((in_html_file));
            Document doc = Jsoup.parse(page_file, "UTF-8", page_url);

            String title = doc.title();
            System.out.println("title: " + title);

            String clean_title = EmojiParser.removeAllEmojis(title);
            clean_title = clean_title.replaceAll("[\\\\/:*?\"<>|]", "");
            if (clean_title.length() > 0) {
                out_html_file = curr_dir + File.separatorChar + clean_title + ".html";
            }

            // css style files
            Elements css_styles = doc.select("link[rel=\"stylesheet\"][type=\"text/css\"]");

            System.out.println("\ncss files: ");

            for (Element css : css_styles) {
                String css_file = css.attr("href");
                System.out.println(css_file);

                Element elem = new Element(Tag.valueOf("style"), "");

                String css_str = readFileToString(curr_dir + File.separatorChar + css_file);
                System.out.println("css_str.length() = " + css_str.length());

                elem.append(css_str);

                Element parent = css.parent();
                parent.appendChild(elem);

                css.remove();
            }

            // javascript files
            Elements js_files = doc.select("script");

            System.out.println("\njavascript files: ");

            for (Element js : js_files) {
                String js_file = js.attr("src");
                System.out.println(js_file);

                js.remove();
            }

            // images
            Elements img_files = doc.select("img");

            System.out.println("\nimg files: ");

            for (Element img : img_files) {
                String img_file = img.attr("src");
                System.out.println(img_file);

                String img_base64 = readImageContentAsBase64(curr_dir + File.separatorChar + img_file);

                String mimeType = detectMimeType(curr_dir + File.separatorChar + img_file);

                // src="data:image/png;base64,......"
                String src_value = "data:" + mimeType + ";base64," + img_base64;

                Element elem = new Element(Tag.valueOf("img"), "");
                elem.attr("src", src_value);

                Element parent = img.parent();
                parent.appendChild(elem);

                img.remove();
            }

            System.out.println("css files: " + css_styles.size());
            System.out.println("js  files: " + js_files.size());
            System.out.println("img files: " + img_files.size());

            writeStringToFile(doc.outerHtml(), out_html_file);
            System.out.println("\nresult fat html saved to: " + out_html_file);

            success = true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return success;
    }

    public static void SaveHtmlToFile(String html_str, String file_path) {
        try (Writer out = new BufferedWriter(new OutputStreamWriter
                (new FileOutputStream(file_path), StandardCharsets.UTF_8))) {
            out.write(html_str);
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
        }
    }

    public static String ReadTextFile(String file_path) {
        StringBuilder sb = new StringBuilder();
        Charset charset = StandardCharsets.UTF_8;
        try {
            List<String> lines = Files.readAllLines(new File(file_path).toPath(), charset);
            for (String line : lines) {
                sb.append(line).append("\n");
            }
        } catch (IOException ex) {
            System.out.println("I/O Exception: " + ex);
        }
        return sb.toString();
    }

    // https://www.techiedelight.com/read-file-contents-with-apache-commons-io-library-java/
    public static String readFileToString(String file_path) {
        String content = "";
        try {
            content = FileUtils.readFileToString(new File(file_path), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static void writeStringToFile(String data_to_write, String file_path) {
        try {
            FileUtils.writeStringToFile(new File(file_path), data_to_write, StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readImageContentAsBase64(String img_file) {
        String img_base64 = "";
        try {
            byte[] img_content = FileUtils.readFileToByteArray(new File(img_file));
            img_base64 = Base64.getEncoder().encodeToString(img_content);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return img_base64;
    }

    public static String detectMimeType(String file_path) {
        final Tika tika = new Tika();
        return tika.detect(file_path);
    }

    public static boolean execSystemCommand(String[] cmd_with_args) {
        boolean success = false;
        try {
            System.out.println("building command");
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(cmd_with_args);
            builder.directory(new File(System.getProperty("user.home")));

            System.out.println("starting process");
            Process process = builder.inheritIO().start();

            System.out.println("waiting");
            int status_code = process.waitFor();
            if (status_code == 0) {
                success = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("returning");
        return success;
    }
}
