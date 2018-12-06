package X4XMLJS;

import java.awt.EventQueue;
import java.awt.ScrollPane;

import javax.swing.JFrame;
import java.awt.BorderLayout;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import java.awt.event.KeyEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;
import java.awt.event.InputEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Component;
import java.awt.Dimension;

import javax.swing.Box;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JSplitPane;

public class EditorGui {
	final JFileChooser fc = new JFileChooser();
	private JFrame frame;
	private LinkedList<XML2JS> converters = new LinkedList<XML2JS>();
	JTabbedPane tabbedPane;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					EditorGui window = new EditorGui();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public EditorGui() {
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.setBounds(100, 100, 653, 553);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout(0, 0));
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		frame.getContentPane().add(tabbedPane, BorderLayout.CENTER);
		
		JMenuBar menuBar = new JMenuBar();
		frame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		menuBar.add(mnFile);
		
		JMenuItem mntmNew = new JMenuItem("New");
		mntmNew.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, InputEvent.CTRL_MASK));
		mnFile.add(mntmNew);
		
		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fc.addChoosableFileFilter(new FileNameExtensionFilter("XML / Script", "xml", "script"));
//				fc.addChoosableFileFilter(new FileNameExtensionFilter("XML Script", "xml.script"));
				fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
				fc.setAcceptAllFileFilterUsed(false);
				int returnval = fc.showOpenDialog(fc);
				
				if(returnval == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
		            System.out.println("Opening: " + file.getPath());
		            
		            try {
		            	// read .xml.script
		            	if(file.getPath().endsWith(".script")) {
		            		JJsXmlSplitPane splitPane = new JJsXmlSplitPane();
				            splitPane.setDividerLocation(550);
				            splitPane.setResizeWeight(1);
				            
				            splitPane.xml2js = new XML2JS();
				            splitPane.xml2js.saved=true;
				            
				            byte[] encoded = Files.readAllBytes(file.toPath());
				            splitPane.JS.setText(new String(encoded, Charset.defaultCharset()));
				            
				            // .xml file exists?
				            File f = new File(file.getPath().replaceAll(".xml.script", ".xml"));
				            if(f.exists()) {
				            	splitPane.xml2js.saveloc=f.getPath();
				            	tabbedPane.addTab(f.getName(), null, splitPane, null);
				            }else{
				            	splitPane.xml2js.saved=false;
				            	splitPane.xml2js.saveloc=f.getPath();
				            	tabbedPane.addTab("*"+f.getName(), null, splitPane, null);
				            }
		            	}else { // read .xml
			            	JJsXmlSplitPane splitPane = new JJsXmlSplitPane(file);
				            splitPane.setDividerLocation(550);
				            splitPane.setResizeWeight(1);
				            
				    		tabbedPane.addTab("*"+file.getName(), null, splitPane, null);
		            	}
		            }catch(Exception ex) {
		            	ex.printStackTrace();
		            }
				}
			}
		});
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mnFile.add(mntmOpen);
		
		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// save!
				JJsXmlSplitPane pane = (JJsXmlSplitPane) tabbedPane.getSelectedComponent();
				if(!pane.xml2js.saved) {
					// determine if saveloc exists
					if(pane.xml2js.saveloc.equals("")) {
						
						// Save file dialog
						fc.resetChoosableFileFilters();
						fc.addChoosableFileFilter(new FileNameExtensionFilter("XML", "xml"));
						int returnval = fc.showSaveDialog(fc);
						
						if(returnval == JFileChooser.APPROVE_OPTION) {
							File file = fc.getSelectedFile();
							pane.xml2js.saveloc=file.getPath();

							// actually save the script first so that a user can come back to it
							File f2 = new File(file.getPath()+".script");
							try {
								f2.createNewFile();
							    BufferedWriter writer = new BufferedWriter(new FileWriter(f2));
							    writer.write(pane.JS.getText());
							    writer.close();
								System.out.println("Saved: " + file.getPath()+".script");
							    
								
								// convert to XML
								try {
									String xml=JS2XML.getXML(pane.JS.getText());
									
									// verify XML
									if(!JS2XML.verifyXML(xml)) {
										// Error out!
									}else {
										// save file
										BufferedWriter writer2 = new BufferedWriter(new FileWriter(file));
									    writer.write(xml);
									    writer.close();
										System.out.println("Saved: " + file.getPath()+".script");
									}
								}catch(Exception e) {
									e.printStackTrace();
								}
							}catch(Exception e) {
								e.printStackTrace();
							}
						}
					}
				}
			}
		});
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mnFile.add(mntmSave);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				System.exit(1);
			}
		});
		
		JMenuItem mntmSaveAs = new JMenuItem("Save As");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mnFile.add(mntmSaveAs);
		
		JMenuItem mntmClose = new JMenuItem("Close");
		mnFile.add(mntmClose);
		mnFile.add(mntmExit);
	}

}

class JJsXmlSplitPane extends JSplitPane{
	public RSyntaxTextArea XML;
	public RSyntaxTextArea JS;
	public XML2JS xml2js;
	
	public JJsXmlSplitPane(File file) throws IOException {
		super();
		//List<String> lines = Files.readAllLines(file.toPath());
    	
		JScrollPane scroller = new JScrollPane();
        JScrollPane scroller_xml = new JScrollPane();
        XML = new RSyntaxTextArea(20, 60);
        XML.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        XML.setCodeFoldingEnabled(true);
        XML.setAntiAliasingEnabled(true);
        
        JS = new RSyntaxTextArea(20, 60);
        JS.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        JS.setCodeFoldingEnabled(true);
        JS.setAntiAliasingEnabled(true);
        
        scroller.setViewportView(JS);
        scroller.setMinimumSize(new Dimension(400,100));
        scroller_xml.setViewportView(XML);
        
        setLeftComponent(scroller);
        setRightComponent(scroller_xml);
        
        byte[] encoded = Files.readAllBytes(file.toPath());
	    XML.setText(new String(encoded, Charset.defaultCharset()));
    	xml2js = new XML2JS(file.getPath());
    	JS.setText(xml2js.getJS());
	}
	
	public JJsXmlSplitPane() {
		JScrollPane scroller = new JScrollPane();
        JScrollPane scroller_xml = new JScrollPane();
        XML = new RSyntaxTextArea(20, 60);
        XML.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        XML.setCodeFoldingEnabled(true);
        XML.setAntiAliasingEnabled(true);
        
        JS = new RSyntaxTextArea(20, 60);
        JS.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        JS.setCodeFoldingEnabled(true);
        JS.setAntiAliasingEnabled(true);
        
        scroller.setViewportView(JS);
        scroller.setMinimumSize(new Dimension(400,100));
        scroller_xml.setViewportView(XML);
        
        setLeftComponent(scroller);
        setRightComponent(scroller_xml);
	}
}
