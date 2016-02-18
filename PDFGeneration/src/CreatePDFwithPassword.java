import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

public class CreatePDFwithPassword {
	public static void main(String[] args) throws DocumentException,
			MalformedURLException, IOException {
		Document document = new Document();
		PdfWriter writer = PdfWriter.getInstance(document,
				new FileOutputStream("PDFwithPasswordOne.pdf"));
		writer.setEncryption("concretepage".getBytes(), "cp123".getBytes(),
				PdfWriter.ALLOW_COPY, PdfWriter.STANDARD_ENCRYPTION_40);
		writer.createXmpMetadata();
		document.open();
		document.add(new Paragraph("This is create PDF with Password demo."));
		document.close();
		System.out.println("Done");
	}
}