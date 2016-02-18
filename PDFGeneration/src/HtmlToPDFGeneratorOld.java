import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.Certificate;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.w3c.tidy.Tidy;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.security.BouncyCastleDigest;
import com.itextpdf.text.pdf.security.ExternalDigest;
import com.itextpdf.text.pdf.security.ExternalSignature;
import com.itextpdf.text.pdf.security.MakeSignature;
import com.itextpdf.text.pdf.security.MakeSignature.CryptoStandard;
import com.itextpdf.text.pdf.security.PrivateKeySignature;
import com.itextpdf.tool.xml.Pipeline;
import com.itextpdf.tool.xml.XMLWorker;
import com.itextpdf.tool.xml.XMLWorkerHelper;
import com.itextpdf.tool.xml.exceptions.CssResolverException;
import com.itextpdf.tool.xml.html.Tags;
import com.itextpdf.tool.xml.parser.XMLParser;
import com.itextpdf.tool.xml.pipeline.css.CSSResolver;
import com.itextpdf.tool.xml.pipeline.css.CssResolverPipeline;
import com.itextpdf.tool.xml.pipeline.end.PdfWriterPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipeline;
import com.itextpdf.tool.xml.pipeline.html.HtmlPipelineContext;

public class HtmlToPDFGeneratorOld {

	public static String getHTMLResponse(String url) throws IOException {
		/*
		 * int byteSize=10 * 1048576; //assuming 10MB max response byte[]
		 * responseBody=new byte[byteSize];
		 */
		String responseBody = null;
		// Create an instance of HttpClient.
		CloseableHttpClient httpclient = HttpClients.createDefault();
		try {
			// Create a method instance.
			HttpGet httpget = new HttpGet(url);

			// Provide custom retry handler is necessary
			/*
			 * method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
			 * new DefaultHttpMethodRetryHandler(3, false));
			 */
			// Create a custom response handler
			ResponseHandler<String> responseHandler = new ResponseHandler<String>() {

				@Override
				public String handleResponse(final HttpResponse response)
						throws ClientProtocolException, IOException {
					int status = response.getStatusLine().getStatusCode();
					if (status >= 200 && status < 300) {
						HttpEntity entity = response.getEntity();
						return entity != null ? EntityUtils.toString(entity)
								: null;
					} else {
						throw new ClientProtocolException(
								"Unexpected response status: " + status);
					}
				}

			};
			responseBody = httpclient.execute(httpget, responseHandler);

			System.out.println(responseBody);

		} catch (IOException e) {
			System.err.println("Fatal transport error: " + e.getMessage());
			e.printStackTrace();
		} catch (Exception e) {
			System.err.println("Fatal protocol violation: " + e.getMessage());
			e.printStackTrace();
		} finally {
			httpclient.close();
		}
		return responseBody;
	}

	public static void generate(String htmlResponse) {

		try {
			// String k = "<html><body> This is my Project </body></html>";
			OutputStream file = new FileOutputStream(new File("D://Test.pdf"));
			Document document = new Document();
			PdfWriter writer = PdfWriter.getInstance(document, file);
			document.open();
			// InputStream is = new ByteArrayInputStream(k.getBytes());

			XMLWorkerHelper worker = XMLWorkerHelper.getInstance();
			worker.parseXHtml(writer, document, new StringReader(htmlResponse));

			/*
			 * HTMLWorker htmlWorker = new HTMLWorker(document);
			 * htmlWorker.parse(new StringReader(htmlResponse));
			 */
			// XMLWorkerHelper.getInstance().parseXHtml(writer, document, is);
			document.close();
			file.close();
			System.out.println("PDF generated successfully");
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	public static void generateWithPipeline(String htmlResponse)
			throws IOException, DocumentException,
			CssResolverException {
		Document document = new Document();
		PdfWriter writer = PdfWriter.getInstance(document,
				new FileOutputStream("D://result.PDF"));
		document.open();

		HtmlPipelineContext htmlContext = new HtmlPipelineContext(null);
		htmlContext.setTagFactory(Tags.getHtmlTagProcessorFactory());
		CSSResolver cssResolver = XMLWorkerHelper.getInstance()
				.getDefaultCssResolver(true);

		Pipeline<?> pipeline = new CssResolverPipeline(cssResolver,
				new HtmlPipeline(htmlContext, new PdfWriterPipeline(document,
						writer)));
		XMLWorker worker = new XMLWorker(pipeline, true);
		XMLParser p = new XMLParser(worker);
		//File input = new File(infile);
		p.parse(new StringReader(htmlResponse));
		document.close();
System.out.println("@@@@@@@@@@@@@@@@@@@@@@@@@@");
		
	}

	public static void addDigitalSignature() {
		try {

			String path = "D:\\sarav\\personal\\lib\\privateKey.store";
			String keystore_password = "password";
			String key_password = "password";
			KeyStore ks = KeyStore.getInstance("pkcs12", "BC");
			ks.load(new FileInputStream(path), keystore_password.toCharArray());
			String alias = (String) ks.aliases().nextElement();
			PrivateKey pk = (PrivateKey) ks.getKey(alias,
					key_password.toCharArray());
			Certificate[] chain = ks.getCertificateChain(alias);

			PdfReader reader = new PdfReader("D://Test.pdf");
			FileOutputStream os = new FileOutputStream("D://result.pdf");
			PdfStamper stamper = PdfStamper.createSignature(reader, os, '\0');
			// appearance
			PdfSignatureAppearance appearance = stamper
					.getSignatureAppearance();
			appearance.setImage(Image
					.getInstance("D:\\sarav\\personal\\lib\\test.jpg"));
			appearance.setReason("Sarav tried this.");
			appearance.setLocation("Foobar");
			appearance.setVisibleSignature(new Rectangle(72, 732, 144, 780), 1,
					"first");

			// digital signature
			ExternalSignature es = new PrivateKeySignature(pk, "SHA-256", "BC");
			ExternalDigest digest = new BouncyCastleDigest();
			MakeSignature.signDetached(appearance, digest, es, chain, null,
					null, null, 0, CryptoStandard.CMS);

			System.out.println("Signed successfully");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException, DocumentException, CssResolverException {

		Security.addProvider(new BouncyCastleProvider());

		String htmlResponse = getHTMLResponse("http://www.test.com"); // test URL

		/*Tidy tidy = new Tidy();
		StringWriter writer = new StringWriter();
		tidy.parse(
				new StringReader(
						"http://www.avisworld.com/avisonline/terms.nsf/TermsByCountryAndLngCategories/US-GB-Common?OpenDocument"),
				writer);
		System.out.println("Errors :" + tidy.getParseErrors());*/
		generateWithPipeline(htmlResponse);
		// generate(htmlResponse);
		// addDigitalSignature();

	}

}
