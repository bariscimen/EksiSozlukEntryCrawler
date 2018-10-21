package bariscimen.com;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import java.io.FileWriter;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class App {
    private JButton downloadButton;
    private JProgressBar progressBar1;
    private JTextField textField1;
    private JPanel panel1;
    private JPanel panel2;

    private String topic_no;
    private Integer max_pages;
    private Integer current_page;
    private String topic;
    private String url;
    private boolean valid_url;

    private List<Entry> all_entries = new ArrayList<Entry>();

    private App() {
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                SwingWorker swingWorker = new SwingWorker() {

                    @Override
                    protected Object doInBackground() throws Exception {

                        url = textField1.getText();
                        url = url.split("\\?")[0];

                        Pattern p = Pattern.compile(".*-(\\d+)(\\?.*|$)");
                        Matcher m = p.matcher(url);

                        if (!m.find()) {
                            valid_url = false;
                            JOptionPane.showMessageDialog(null, "Hatalı URL!","Hata!", JOptionPane.ERROR_MESSAGE);
                        } else {
                            valid_url = true;
                            topic_no = m.group(1);
                            Document doc = App.getPage(url);
                            String title = doc.title();

                            Element pager = doc.select("div.pager").first();
                            max_pages = Integer.parseInt(pager.attr("data-pagecount"));
                            current_page = Integer.parseInt(pager.attr("data-currentpage"));

                            progressBar1.setMaximum(max_pages);
                            progressBar1.setMinimum(0);
                            progressBar1.setValue(current_page);
                            progressBar1.setStringPainted(true);
                            //progressBar1.setBounds(40, 40, 160, 30);
                            progressBar1.setString("Sayfa: " + current_page + "/" + max_pages);

                            topic = doc.select("span[itemprop=name]").first().text();
                            //JOptionPane.showMessageDialog(null, title + " " + max_pages + " " + current_page + " " + topic);
                            scrape_entries(doc);
                            if (max_pages > 1) {
                                for (int i = 2; i <= max_pages; i++) {
                                    current_page = i;
                                    doc = App.getPage(url + "?a=nice&p=" + current_page);
                                    scrape_entries(doc);
                                    publish(current_page);
                                    progressBar1.setValue(current_page);
                                    progressBar1.setString("Sayfa: " + current_page + "/" + max_pages);
                                }
                            }

                            String csvFile = System.getProperty("user.dir") + "/" + topic_no + ".csv";
                            FileWriter writer = new FileWriter(csvFile);
                            writer.write('\ufeff');

                            //for header
                            CSVUtils.writeLine(writer, Arrays.asList(
                                    "ID", "Başlık", "Yazar", "Yazar ID", "İşaretler", "Favori mi?",
                                    "Favori Sayısı", "Yorum Sayısı", "İçerik", "Oluşturulma Tarihi", "Güncelleme Tarihi"
                            ), ';');

                            for (Entry row : all_entries) {

                                List<String> list = new ArrayList<>();
                                list.add(row.id);
                                list.add(row.topic);
                                list.add(row.author);
                                list.add(row.author_id);
                                list.add(row.flags);
                                list.add(row.is_favorite);
                                list.add(row.favorite_count);
                                list.add(row.comment_count);
                                list.add(row.content);
                                list.add(row.created_at);
                                list.add(row.updated_at);

                                CSVUtils.writeLine(writer, list, ';', '"');
                            }

                            writer.flush();
                            writer.close();

                            //JOptionPane.showMessageDialog(null, App.class.getProtectionDomain().getCodeSource().getLocation().getPath());
                            JOptionPane.showMessageDialog(null, "Dosya kaydedildi!\n" + csvFile,"Başarılı!", JOptionPane.INFORMATION_MESSAGE);
                        }
                        return 0;
                    }

                    @Override
                    protected void done() {
                        if (valid_url) {
                            progressBar1.setString("İşlem tamamlandı!");
                            textField1.setText("");
                        }
                        textField1.setEnabled(true);
                        downloadButton.setEnabled(true);
                        //progressBar1.setValue(0);
                        all_entries = new ArrayList<Entry>();
                    }

                    @Override
                    protected void process(List chunks) {
                        for (Object object : chunks) {
                            Integer progress = (Integer) object;
                            progressBar1.setValue(progress);
                        }
                    }

                };

                textField1.setEnabled(false);
                downloadButton.setEnabled(false);
                swingWorker.execute();
            }
        });


    }

    private void scrape_entries(Document doc) {
        Elements entries = doc.select("ul#entry-item-list>li");
        for (Element entry : entries) {
            Entry e = new Entry();
            e.topic = topic;
            e.id = entry.attr("data-id");
            e.author = entry.attr("data-author");
            e.author_id = entry.attr("data-author-id");
            e.flags = entry.attr("data-flags");
            //e.is_favorite = entry.attr("data-isfavorite");
            if (entry.attr("data-isfavorite") == "FALSE") {
                e.is_favorite = "Hayır";
            }else{
                e.is_favorite = "Evet";
            }
            e.favorite_count = entry.attr("data-favorite-count");
            e.comment_count = entry.attr("data-comment-count");
            //entry.selectFirst("div.content").select("br").after("\r\n");
            e.content = entry.selectFirst("div.content").text().trim();
            //e.date = entry.selectFirst("a.entry-date.permalink").text().trim();
            String[] date = entry.selectFirst("a.entry-date.permalink").text().trim().split(" ~ ");
            e.created_at = date[0];
            if (date.length>1) {
                if (date[1].trim().length()==5)
                    e.updated_at = date[0].split(" ")[0] + " " + date[1];
                else
                    e.updated_at = date[1];
            }

            all_entries.add(e);
            //JOptionPane.showMessageDialog(null, e);
        }
    }

    private static Document getPage(String url) {
        while (true) {
            try {
                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                        .referrer("http://www.google.com")
                        .ignoreHttpErrors(true)
                        .get();
                return doc;
            } catch (HttpStatusException e) {
                //e.printStackTrace();
                break;
            } catch (SocketTimeoutException e) {
                //e.printStackTrace();
                // try again
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }

        return null;
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Ekşisözlük İndir v0.1 -- https://github.com/bariscimen/JEksiSozlukCrawler");
        frame.setContentPane(new App().panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }
}
