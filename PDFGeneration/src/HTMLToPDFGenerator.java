

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.htmlcleaner.CleanerProperties;
import org.htmlcleaner.CommentNode;
import org.htmlcleaner.HtmlCleaner;
import org.htmlcleaner.HtmlNode;
import org.htmlcleaner.PrettyXmlSerializer;
import org.htmlcleaner.TagNode;
import org.htmlcleaner.TagNodeVisitor;
import org.htmlcleaner.Utils;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.DigestAlgorithms;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import com.itextpdf.text.pdf.security.PrivateKeySignature;
import com.lowagie.text.DocumentException;




/**
 * 
 * @author Sarav
 */
public class HTMLToPDFGenerator {

	/**
	 * @param args
	 */
	static int cssCounter = 0;
	public static final String KEYSTORE = "ks";
	public static final char[] PASSWORD = "password".toCharArray();
	public static final String DEST = "D:/new_result.pdf";
	public static final  String site = "http://www.sarav.com";
	public static final  String page = "XXXXXX";
	public static final  String cssUrl = "http://www.sarav.com";
	public static final  String temp_path = "D:/";

	public static void main(String[] args) {

		
		generateHtmlToPDF(site, page, cssUrl, temp_path);

	}
	/**
	 * @param site
	 * @param pageUrl
	 * @param cssUrl
	 * @param temp_path
	 */
	private static void generateHtmlToPDF(final String site, String pageUrl,
			final String cssUrl, final String temp_path) {
		try {
			URL url = new URL(site + pageUrl);

			CleanerProperties props = new CleanerProperties();

			// HTMLCleaner part
			// set some properties to non-default values
			props.setTranslateSpecialEntities(true);
			props.setTransResCharsToNCR(true);
			props.setOmitComments(true);

			// do parsing
			TagNode tagNode = new HtmlCleaner(props).clean(url);
			tagNode.traverse(new TagNodeVisitor() {

				public boolean visit(TagNode tagNode, HtmlNode htmlNode) {
					if (htmlNode instanceof TagNode) {
						TagNode tag = (TagNode) htmlNode;
						String tagName = tag.getName();

						if ("img".equals(tagName)) {
							String src = tag.getAttributeByName("src");
							if (src != null && !src.startsWith("http")) {
								tag.setAttribute("src",
										Utils.fullUrl(site, src));
							}
						}
						if ("a".equals(tagName)) {
							String src = tag.getAttributeByName("href");
							if (src != null && !src.startsWith("http")) {
								tag.setAttribute("href",
										org.htmlcleaner.Utils.fullUrl(site, src));
							}
						}
						if ("link".equals(tagName)) {
							String rel = tag.getAttributeByName("rel");
							String href = tag.getAttributeByName("href");
							if (href != null
									&& "Stylesheet".equalsIgnoreCase(rel)) {
								try {
									HttpClient client = new DefaultHttpClient();
									String fullUrl = "";
									if (href.startsWith("http"))
										fullUrl = href;
									else
										fullUrl = Utils.fullUrl(cssUrl, href);
									HttpGet get = new HttpGet(fullUrl);
									HttpResponse response = client.execute(get);
									HttpEntity entity = response.getEntity();
									if (entity != null) {
										InputStream is = entity.getContent();
										href = temp_path + "css" + cssCounter
												+ ".css";
										cssCounter++;
										OutputStream os = new FileOutputStream(
												href);
										IOUtils.copy(is, os);
										
										os.close();
										is.close();
									}
									tag.setAttribute("href", href);
								} catch (IOException ex) {
									Logger.getLogger(
											HTMLToPDFGenerator.class.getName())
											.log(Level.SEVERE, null, ex);
								}
							}
						}
					} else if (htmlNode instanceof CommentNode) {
						CommentNode comment = ((CommentNode) htmlNode);
						comment.getContent().append(" -- By HtmlCleaner");
					}
					// tells visitor to continue traversing the DOM tree
					return true;
				}
			});

			// serialize to xml file
			new PrettyXmlSerializer(props).writeToFile(tagNode, temp_path
					+ "samplepage.xhtml", "utf-8");

			// FlyingSaucer and iText part
			String inputFile = temp_path + "samplepage.xhtml";
			String url2 = new File(inputFile).toURI().toURL().toString();
			String outputFile = temp_path + "test-"+new Random().nextInt()+".pdf";
			OutputStream os = new FileOutputStream(outputFile);

			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocument(url2);
			renderer.layout();
			renderer.createPDF(os);

			os.close();
			
			//attachSignatureToPDF(outputFile);

		} catch (DocumentException ex) {
			Logger.getLogger(HTMLToPDFGenerator.class.getName()).log(
					Level.SEVERE, null, ex);
		} catch (IOException ex) {
			Logger.getLogger(HTMLToPDFGenerator.class.getName()).log(
					Level.SEVERE, null, ex);

		} /*catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (com.itextpdf.text.DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	/**
	 * @param src
	 * @param dest
	 * @param chain
	 * @param pk
	 * @param digestAlgorithm
	 * @param provider
	 * @param subfilter
	 * @param reason
	 * @param location
	 * @throws GeneralSecurityException
	 * @throws IOException
	 * @throws DocumentException
	 * @throws com.itextpdf.text.DocumentException
	 */
	private static void sign(String src, String dest, Certificate[] chain, PrivateKey pk, String digestAlgorithm,
			String provider, CryptoStandard subfilter, String reason, String location)
					throws GeneralSecurityException, IOException, DocumentException, com.itextpdf.text.DocumentException {
		// Creating the reader and the stamper
		PdfReader reader = new PdfReader(src);
		FileOutputStream os = new FileOutputStream(dest);
		PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');
		// Creating the appearance
		PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
		appearance.setReason(reason);
		appearance.setLocation(location);
		appearance.setVisibleSignature(new Rectangle(100, 748, 100, 780), 1, "sig");
		// Creating the signature
		ExternalDigest digest = new BouncyCastleDigest();
		ExternalSignature signature = new PrivateKeySignature(pk, digestAlgorithm, provider);
		MakeSignature.signDetached(appearance, digest, signature, chain, null, null, null, 0, subfilter);
		
	}
	
	/**
	 * @param SRC
	 * @throws GeneralSecurityException
	 * @throws IOException
	 * @throws DocumentException
	 * @throws com.itextpdf.text.DocumentException
	 */
	private static void attachSignatureToPDF(String SRC) throws GeneralSecurityException, IOException, DocumentException, com.itextpdf.text.DocumentException {
		BouncyCastleProvider provider = new BouncyCastleProvider();
		Security.addProvider(provider);
		KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
		ks.load(new FileInputStream(KEYSTORE), PASSWORD);
		String alias = (String) ks.aliases().nextElement();
		PrivateKey pk = (PrivateKey) ks.getKey(alias, PASSWORD);
		Certificate[] chain = ks.getCertificateChain(alias);

	sign(SRC, String.format(DEST, 1), chain, pk, DigestAlgorithms.SHA256, provider.getName(),
				CryptoStandard.CMS, "Terms & Conditions", "Brekline, United Kingdom");
		/*app.sign(SRC, String.format(DEST, 2), chain, pk, DigestAlgorithms.SHA512, provider.getName(),
				CryptoStandard.CMS, "Test 2", "Ghent");
		app.sign(SRC, String.format(DEST, 3), chain, pk, DigestAlgorithms.SHA256, provider.getName(),
				CryptoStandard.CADES, "Test 3", "Ghent");
		app.sign(SRC, String.format(DEST, 4), chain, pk, DigestAlgorithms.RIPEMD160, provider.getName(),
				CryptoStandard.CADES, "Test 4", "Ghent");*/
		
	}

}
