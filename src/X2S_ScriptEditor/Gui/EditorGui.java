package X2S_ScriptEditor.Gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.LinkedList;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.DefaultCaret;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import X2S_ScriptEditor.Script.XML2JS;
import X2S_ScriptEditor.XML.JS2XML;

public class EditorGui {
	final JFileChooser fc = new JFileChooser();
	private JFrame frame;
	private LinkedList<XML2JS> converters = new LinkedList<XML2JS>();
	JTabbedPane tabbedPane;
	DIFFGui diffgui = null;

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
		frame.setTitle("X2S XML Script Editor");
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
				openFile();
			}
		});
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mnFile.add(mntmOpen);
		
		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				saveFile();
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
		
		JMenu mnTools = new JMenu("Tools");
		menuBar.add(mnTools);
		
		JMenuItem mntmXmlDiffCreator = new JMenuItem("XML DIFF Creator");
		mntmXmlDiffCreator.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(diffgui == null)
					diffgui = new DIFFGui();
				
				diffgui.frame.show();
			}
		});
		mnTools.add(mntmXmlDiffCreator);
	}

	private void openFile() {
		fc.addChoosableFileFilter(new FileNameExtensionFilter("XML / Script", "xml", "script"));
//				fc.addChoosableFileFilter(new FileNameExtensionFilter("XML Script", "xml.script"));
		fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
		fc.setAcceptAllFileFilterUsed(false);
		fc.setCurrentDirectory(new File(System.getProperty("user.dir")));
		
		// open dialog
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
		            splitPane.EDITORLEFT.setText(new String(encoded, Charset.defaultCharset()));
		            
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

	private void saveFile() {
		// save!
		JJsXmlSplitPane pane = (JJsXmlSplitPane) tabbedPane.getSelectedComponent();
		if(!pane.xml2js.saved) {
			// determine if saveloc exists
			if(pane.xml2js.saveloc.equals("")) {
				
				// Save file dialog
				fc.resetChoosableFileFilters();
				fc.addChoosableFileFilter(new FileNameExtensionFilter("XML", "xml"));
				if(!pane.xml2js.saveloc.equals(""))  // set default location to look in
					fc.setCurrentDirectory(new File(pane.xml2js.saveloc));
				int returnval = fc.showSaveDialog(fc);
				
				if(returnval == JFileChooser.APPROVE_OPTION) {
					File file = fc.getSelectedFile();
					pane.xml2js.saveloc=file.getPath();

					// actually save the script first so that a user can come back to it
					File f2 = new File(file.getPath()+".script");
					try {
						f2.createNewFile();
					    BufferedWriter writer = new BufferedWriter(new FileWriter(f2));
					    writer.write(pane.EDITORLEFT.getText());
					    writer.close();
						System.out.println("Saved: " + file.getPath()+".script");
					    
						
						// convert to XML
						try {
							String xml=JS2XML.getXML(pane.EDITORLEFT.getText());
							
							// verify XML
							if(!JS2XML.verifyXML(xml)) {
								// Error out!
							}else {
								// save file
								BufferedWriter writer2 = new BufferedWriter(new FileWriter(file));
							    writer2.write(xml);
							    writer2.close();
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

}

class JJsXmlSplitPane extends JSplitPane{
	public RSyntaxTextArea EDITORRIGHT;
	public RSyntaxTextArea EDITORLEFT;
	public JScrollPane SCROLLLEFT, SCROLLRIGHT;
	public XML2JS xml2js;
	public JS2XML js2xml;
	
	public JJsXmlSplitPane(File file) throws IOException {
		super();
		initialize();
        
		
		// determine panes based on file name?
		
        //byte[] encoded = Files.readAllBytes(file.toPath());
        xml2js = new XML2JS(file.getPath());
    	js2xml = new JS2XML();
    	String js = xml2js.getJS();
    	
    	setLeft(js);
    	setRight(js2xml.getXML(js));
    	
	}
	
	public JJsXmlSplitPane() {
		super();
		
		initialize();
	}
	
	public void setLeft(String jstext) {
		Point v = SCROLLLEFT.getLocation();
		int x = v.x;
		int y = v.y;
		EDITORLEFT.setText(jstext);
		SCROLLLEFT.setLocation(x, y);
	}
	
	public void setRight(String xmltext) {
		Point v = SCROLLRIGHT.getLocation();
		int x = v.x;
		int y = v.y;
		EDITORRIGHT.setText(xmltext);
		SCROLLRIGHT.setLocation(x, y);
	}
	
	void initialize() {
		SCROLLLEFT = new JScrollPane();
        SCROLLRIGHT = new JScrollPane();
        EDITORLEFT = new RSyntaxTextArea(20, 60);
        EDITORRIGHT = new RSyntaxTextArea(20, 60);
        
        
        EDITORRIGHT.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        EDITORRIGHT.setCodeFoldingEnabled(true);
        EDITORRIGHT.setAntiAliasingEnabled(true);
        EDITORRIGHT.addKeyListener(new JJsXmlLeftPaneKeyListener(this, false));
        
        EDITORLEFT.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT);
        EDITORLEFT.setCodeFoldingEnabled(true);
        EDITORLEFT.setAntiAliasingEnabled(true);
        EDITORLEFT.addKeyListener(new JJsXmlLeftPaneKeyListener(this, true));
        
        SCROLLLEFT.setViewportView(EDITORLEFT);
        SCROLLLEFT.setMinimumSize(new Dimension(400,100));
        SCROLLRIGHT.setViewportView(EDITORRIGHT);
        
        setLeftComponent(SCROLLLEFT);
        setRightComponent(SCROLLRIGHT);
	}
}

class JJsXmlLeftPaneKeyListener implements KeyListener{
	private JJsXmlSplitPane splitpane;
	private boolean isLeft=false;
	public JJsXmlLeftPaneKeyListener(JJsXmlSplitPane _splitpane, boolean _isLeft) {
		splitpane = _splitpane;
		isLeft=_isLeft;
	}
	
	@Override
	public void keyTyped(KeyEvent e) {
		// TODO Auto-generated method stub
		// update right pane with changes on left pane
		
		if(isLeft) {
			// left side changed
			System.out.println("left side changed");
			
			splitpane.setRight(splitpane.js2xml.getXML(splitpane.EDITORLEFT.getText()));
			
		}else {
			// right side changed
			System.out.println("right side changed");
			
			splitpane.setLeft(splitpane.xml2js.getJS(splitpane.EDITORRIGHT.getText()));
		}
	}

	@Override
	public void keyPressed(KeyEvent e) {
		// do nothing
	}

	@Override
	public void keyReleased(KeyEvent e) {
		// do nothing
	}
	
}

