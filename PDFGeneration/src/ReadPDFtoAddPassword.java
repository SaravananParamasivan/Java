import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
public class ReadPDFtoAddPassword {
	public static void main(String[] args) throws FileNotFoundException, DocumentException, IOException {
		
		Document document = new Document();
		PdfWriter writer = PdfWriter.getInstance(document, new FileOutputStream("tmp.pdf"));
		document.open();
        document.add(new Paragraph("This is create PDF with Password demo."));
        document.close();
		
	      PdfReader reader = new PdfReader("tmp.pdf");
	      PdfStamper stamper = new PdfStamper(reader, new FileOutputStream("PDFwithPasswordTwo.pdf"));
	      stamper.setEncryption("pwd123".getBytes(), "cp123".getBytes(),PdfWriter.ALLOW_COPY, PdfWriter.STRENGTH128BITS);
	      stamper.close();
	      reader.close();
	      System.out.println("Testing Done");
	}
} 