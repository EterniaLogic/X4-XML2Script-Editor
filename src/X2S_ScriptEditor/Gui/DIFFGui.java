package X2S_ScriptEditor.Gui;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;

import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rsyntaxtextarea.SyntaxConstants;

import X2S_ScriptEditor.Script.XML2JS;

public class DIFFGui {
	final JFileChooser fc = new JFileChooser();
	JFrame frame;
	JTabbedPane tabbedPane;
	
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					DIFFGui window = new DIFFGui();
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}
	
	
	public DIFFGui() {
		initialize();
	}
	
	private void initialize() {
		frame = new JFrame();
		frame.setTitle("X2S XML Difference Creator");
		frame.setBounds(100, 100, 653, 553);
		frame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
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
				// openFile();
			}
		});
		mntmOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O, InputEvent.CTRL_MASK));
		mnFile.add(mntmOpen);
		
		JMenuItem mntmSave = new JMenuItem("Save");
		mntmSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				// saveFile(); // save XML Difference file
			}
		});
		mntmSave.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK));
		mnFile.add(mntmSave);
		
		JMenuItem mntmExit = new JMenuItem("Exit");
		mntmExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				frame.hide();
			}
		});
		
		JMenuItem mntmSaveAs = new JMenuItem("Save As");
		mntmSaveAs.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_S, InputEvent.CTRL_MASK | InputEvent.ALT_MASK));
		mnFile.add(mntmSaveAs);
		
		JMenuItem mntmClose = new JMenuItem("Close");
		mnFile.add(mntmClose);
		mnFile.add(mntmExit);
		
		
		
		
		
		// two dialogs, two buttons
		JJsXmlSplitPane_DIFF splitPane = new JJsXmlSplitPane_DIFF();
		splitPane.EDITORLEFT.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent arg0) {
				changedXML();
			}
		});
		
		
		
        splitPane.EDITORLEFT.setText("<!-- original -->");
        splitPane.EDITORRIGHT.setText("<!-- changed -->");
        splitPane.EDITORBOTTOM.setText("<!-- DIFF -->");
        
        splitPane.EDITORLEFT.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        
        tabbedPane.add("*Untitled", splitPane);
	}
	
	private void changedXML() {
		// do an XML Diff
	}
}


class JJsXmlSplitPane_DIFF extends JSplitPane{
	public RSyntaxTextArea EDITORLEFT; // Original
	public RSyntaxTextArea EDITORRIGHT; // New changes
	public RSyntaxTextArea EDITORBOTTOM; // for DIFF
	public XML2JS xml2js;
	
	public JJsXmlSplitPane_DIFF(File file) throws IOException {
		super();
		initialize();
        
        byte[] encoded = Files.readAllBytes(file.toPath());
	    EDITORRIGHT.setText(new String(encoded, Charset.defaultCharset()));
    	xml2js = new XML2JS(file.getPath());
    	EDITORLEFT.setText(xml2js.getJS());
	}
	
	public JJsXmlSplitPane_DIFF() {
		super();
		
		initialize();
	}
	
	void initialize() {
		JSplitPane mainhorizpane = new JSplitPane();
		
		
		
		JScrollPane scroller_left = new JScrollPane();
        JScrollPane scroller_right = new JScrollPane();
        EDITORRIGHT = new RSyntaxTextArea(20, 60);
        EDITORRIGHT.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        EDITORRIGHT.setCodeFoldingEnabled(true);
        EDITORRIGHT.setAntiAliasingEnabled(true);
        
        EDITORLEFT = new RSyntaxTextArea(20, 60);
        EDITORLEFT.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        EDITORLEFT.setCodeFoldingEnabled(true);
        EDITORLEFT.setAntiAliasingEnabled(true);
        
        JScrollPane scroller_diff_xml = new JScrollPane();
        EDITORBOTTOM = new RSyntaxTextArea(20, 60);
        EDITORBOTTOM.setSyntaxEditingStyle(SyntaxConstants.SYNTAX_STYLE_XML);
        EDITORBOTTOM.setCodeFoldingEnabled(true);
        EDITORBOTTOM.setAntiAliasingEnabled(true);
        
        scroller_left.setViewportView(EDITORLEFT);
        scroller_left.setMinimumSize(new Dimension(400,100));
        scroller_right.setViewportView(EDITORRIGHT);
        scroller_diff_xml.setViewportView(EDITORBOTTOM);
        
        mainhorizpane.setLeftComponent(scroller_left);
        mainhorizpane.setRightComponent(scroller_right);
        
        mainhorizpane.setDividerLocation(0.5);
        mainhorizpane.setResizeWeight(0.5);
        
        this.setOrientation(JSplitPane.VERTICAL_SPLIT);
        this.setTopComponent(mainhorizpane);
        this.setBottomComponent(scroller_diff_xml);
        this.setDividerLocation(0.7);
        this.setResizeWeight(1);
	}
}