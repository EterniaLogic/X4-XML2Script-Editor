/*
 * 07/14/2014
 *
 * NSISTokenMaker.java - Scanner for NSIS installer scripts.
 * 
 * This library is distributed under a modified BSD license.  See the included
 * RSyntaxTextArea.License.txt file for details.
 */
package org.fife.ui.rsyntaxtextarea.modes;

import java.io.*;
import javax.swing.text.Segment;

import org.fife.ui.rsyntaxtextarea.*;


/**
 * Scanner for NSIS installer scripts.<p>
 *
 * This implementation was created using
 * <a href="http://www.jflex.de/">JFlex</a> 1.4.1; however, the generated file
 * was modified for performance.  Memory allocation needs to be almost
 * completely removed to be competitive with the handwritten lexers (subclasses
 * of <code>AbstractTokenMaker</code>, so this class has been modified so that
 * Strings are never allocated (via yytext()), and the scanner never has to
 * worry about refilling its buffer (needlessly copying chars around).
 * We can achieve this because RText always scans exactly 1 line of tokens at a
 * time, and hands the scanner this line as an array of characters (a Segment
 * really).  Since tokens contain pointers to char arrays instead of Strings
 * holding their contents, there is no need for allocating new memory for
 * Strings.<p>
 *
 * The actual algorithm generated for scanning has, of course, not been
 * modified.<p>
 *
 * If you wish to regenerate this file yourself, keep in mind the following:
 * <ul>
 *   <li>The generated <code>NSISTokenMaker.java</code> file will contain two
 *       definitions of both <code>zzRefill</code> and <code>yyreset</code>.
 *       You should hand-delete the second of each definition (the ones
 *       generated by the lexer), as these generated methods modify the input
 *       buffer, which we'll never have to do.</li>
 *   <li>You should also change the declaration/definition of zzBuffer to NOT
 *       be initialized.  This is a needless memory allocation for us since we
 *       will be pointing the array somewhere else anyway.</li>
 *   <li>You should NOT call <code>yylex()</code> on the generated scanner
 *       directly; rather, you should use <code>getTokenList</code> as you would
 *       with any other <code>TokenMaker</code> instance.</li>
 * </ul>
 *
 * @author Robert Futrell
 * @version 1.0
 *
 */
%%

%public
%class NSISTokenMaker
%extends AbstractJFlexCTokenMaker
%unicode
%ignorecase
%type org.fife.ui.rsyntaxtextarea.Token


%{


	/**
	 * Constructor.  This must be here because JFlex does not generate a
	 * no-parameter constructor.
	 */
	public NSISTokenMaker() {
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addToken(int, int, int)
	 */
	private void addHyperlinkToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, true);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 */
	private void addToken(int tokenType) {
		addToken(zzStartRead, zzMarkedPos-1, tokenType);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param tokenType The token's type.
	 * @see #addHyperlinkToken(int, int, int)
	 */
	private void addToken(int start, int end, int tokenType) {
		int so = start + offsetShift;
		addToken(zzBuffer, start,end, tokenType, so, false);
	}


	/**
	 * Adds the token specified to the current linked list of tokens.
	 *
	 * @param array The character array.
	 * @param start The starting offset in the array.
	 * @param end The ending offset in the array.
	 * @param tokenType The token's type.
	 * @param startOffset The offset in the document at which this token
	 *                    occurs.
	 * @param hyperlink Whether this token is a hyperlink.
	 */
	@Override
	public void addToken(char[] array, int start, int end, int tokenType,
						int startOffset, boolean hyperlink) {
		super.addToken(array, start,end, tokenType, startOffset, hyperlink);
		zzStartRead = zzMarkedPos;
	}


	/**
	 * {@inheritDoc}
	 */
	@Override
	public String[] getLineCommentStartAndEnd(int languageIndex) {
		return new String[] { "#", null };
	}


	/**
	 * Returns the first token in the linked list of tokens generated
	 * from <code>text</code>.  This method must be implemented by
	 * subclasses so they can correctly implement syntax highlighting.
	 *
	 * @param text The text from which to get tokens.
	 * @param initialTokenType The token type we should start with.
	 * @param startOffset The offset into the document at which
	 *        <code>text</code> starts.
	 * @return The first <code>Token</code> in a linked list representing
	 *         the syntax highlighted text.
	 */
	public Token getTokenList(Segment text, int initialTokenType, int startOffset) {

		resetTokenList();
		this.offsetShift = -text.offset + startOffset;

		// Start off in the proper state.
		int state = YYINITIAL;
		switch (initialTokenType) {
			case Token.LITERAL_STRING_DOUBLE_QUOTE:
				state = STRING;
				break;
			case Token.LITERAL_CHAR:
				state = CHAR_LITERAL;
				break;
			case Token.LITERAL_BACKQUOTE:
				state = BACKTICKS;
				break;
			case Token.COMMENT_MULTILINE:
				state = MLC;
				break;
		}

		start = text.offset;
		s = text;
		try {
			yyreset(zzReader);
			yybegin(state);
			return yylex();
		} catch (IOException ioe) {
			ioe.printStackTrace();
			return new TokenImpl();
		}

	}


	/**
	 * Refills the input buffer.
	 *
	 * @return      <code>true</code> if EOF was reached, otherwise
	 *              <code>false</code>.
	 */
	private boolean zzRefill() {
		return zzCurrentPos>=s.offset+s.count;
	}


	/**
	 * Resets the scanner to read from a new input stream.
	 * Does not close the old reader.
	 *
	 * All internal variables are reset, the old input stream 
	 * <b>cannot</b> be reused (internal buffer is discarded and lost).
	 * Lexical state is set to <tt>YY_INITIAL</tt>.
	 *
	 * @param reader   the new input stream 
	 */
	public final void yyreset(Reader reader) {
		// 's' has been updated.
		zzBuffer = s.array;
		/*
		 * We replaced the line below with the two below it because zzRefill
		 * no longer "refills" the buffer (since the way we do it, it's always
		 * "full" the first time through, since it points to the segment's
		 * array).  So, we assign zzEndRead here.
		 */
		//zzStartRead = zzEndRead = s.offset;
		zzStartRead = s.offset;
		zzEndRead = zzStartRead + s.count - 1;
		zzCurrentPos = zzMarkedPos = zzPushbackPos = s.offset;
		zzLexicalState = YYINITIAL;
		zzReader = reader;
		zzAtBOL  = true;
		zzAtEOF  = false;
	}


%}

Letter							= ([A-Za-z])
LetterOrUnderscore				= ({Letter}|"_")
NonzeroDigit						= ([1-9])
Digit							= ("0"|{NonzeroDigit})
HexDigit							= ({Digit}|[A-Fa-f])
OctalDigit						= ([0-7])
EscapedSourceCharacter				= ("u"{HexDigit}{HexDigit}{HexDigit}{HexDigit})
NonSeparator						= ([^\t\f\r\n\ \(\)\{\}\[\]\;\,\.\=\>\<\!\~\?\:\+\-\*\/\&\|\^\%\"\']|"#"|"\\")
IdentifierStart					= ({LetterOrUnderscore}|[$/])
IdentifierPart						= ({IdentifierStart}|{Digit}|("\\"{EscapedSourceCharacter}))

LineTerminator				= (\n)
WhiteSpace				= ([ \t\f])

MLCBegin					= ("/*")
MLCEnd						= ("*/")
LineCommentBegin			= ([;#])

IntegerLiteral				= (({NonzeroDigit}{Digit}*)|"0")
HexLiteral					= ("0"(([xX]{HexDigit}+)|({OctalDigit}*)))
ErrorNumberFormat			= (({IntegerLiteral}|{HexLiteral}){NonSeparator}+)
BooleanLiteral				= ("true"|"false")

Separator					= ([\(\)\{\}\[\]])
Separator2				= ([\;,.])

NonAssignmentOperator		= ("+"|"-"|"<="|"^"|"++"|"<"|"*"|">="|"%"|"--"|">"|"/"|"!="|"?"|">>"|"!"|"&"|"=="|":"|">>"|"~"|"|"|"&&")
AssignmentOperator			= ("="|"-="|"*="|"/="|"|="|"&="|"^="|"+="|"%="|"<<="|">>=")
Operator					= ({NonAssignmentOperator}|{AssignmentOperator})

Identifier				= ({IdentifierStart}{IdentifierPart}*)
VariableStart			= ("$")
Variable				= ({VariableStart}({Identifier}|"{"{Identifier}"}"))

URLGenDelim				= ([:\/\?#\[\]@])
URLSubDelim				= ([\!\$&'\(\)\*\+,;=])
URLUnreserved			= ({LetterOrUnderscore}|{Digit}|[\-\.\~])
URLCharacter			= ({URLGenDelim}|{URLSubDelim}|{URLUnreserved}|[%])
URLCharacters			= ({URLCharacter}*)
URLEndCharacter			= ([\/\$]|{Letter}|{Digit})
URL						= (((https?|f(tp|ile))"://"|"www.")({URLCharacters}{URLEndCharacter})?)


%state STRING
%state CHAR_LITERAL
%state BACKTICKS
%state MLC
%state EOL_COMMENT

%%

<YYINITIAL> {

	/* Keywords */
	"function" |
	"functionend" |
	"section" |
	"sectionend" |
	"subsection" |
	"subsectionend"				{ addToken(Token.RESERVED_WORD); }

	/* Instructions */
	"addbrandingimage" |
	"addsize" |
	"allowrootdirinstall" |
	"allowskipfiles" |
	"autoclosewindow" |
	"bggradient" |
	"brandingtext" |
	"bringtofront" |
	"callinstdll" |
	"caption" |
	"changeui" |
	"checkbitmap" |
	"completedtext" |
	"componenttext" |
	"copyfiles" |
	"crccheck" |
	"createdirectory" |
	"createfont" |
	"createshortcut" |
	"delete" |
	"deleteinisec" |
	"deleteinistr" |
	"deleteregkey" |
	"deleteregvalue" |
	"detailprint" |
	"detailsbuttontext" |
	"dirshow" |
	"dirtext" |
	"enumregkey" |
	"enumregvalue" |
	"exch" |
	"exec" |
	"execshell" |
	"execwait" |
	"expandenvstrings" |
	"file" |
	"fileclose" |
	"fileerrortext" |
	"fileopen" |
	"fileread" |
	"filereadbyte" |
	"fileseek" |
	"filewrite" |
	"filewritebyte" |
	"findclose" |
	"findfirst" |
	"findnext" |
	"findwindow" |
	"flushini" |
	"getcurinsttype" |
	"getcurrentaddress" |
	"getdlgitem" |
	"getdllversion" |
	"getdllversionlocal" |
	"getfiletime" |
	"getfiletimelocal" |
	"getfullpathname" |
	"getfunctionaddress" |
	"getlabeladdress" |
	"gettempfilename" |
	"getwindowtext" |
	"hidewindow" |
	"icon" |
	"initpluginsdir" |
	"installbuttontext" |
	"installcolors" |
	"installdir" |
	"installdirregkey" |
	"instprogressflags" |
	"insttype" |
	"insttypegettext" |
	"insttypesettext" |
	"intfmt" |
	"intop" |
	"langstring" |
	"langstringup" |
	"licensebkcolor" |
	"licensedata" |
	"licenseforceselection" |
	"licensetext" |
	"loadlanguagefile" |
	"loadlanguagefile" |
	"logset" |
	"logtext" |
	"miscbuttontext" |
	"name" |
	"nop" |
	"outfile" |
	"page" |
	"plugindir" |
	"pop" |
	"push" |
	"readenvstr" |
	"readinistr" |
	"readregdword" |
	"readregstr" |
	"regdll" |
	"rename" |
	"requestexecutionlevel" |
	"reservefile" |
	"rmdir" |
	"searchpath" |
	"sectiongetflags" |
	"sectiongetinsttypes" |
	"sectiongetsize" |
	"sectiongettext" |
	"sectionin" |
	"sectionsetflags" |
	"sectionsetinsttypes" |
	"sectionsetsize" |
	"sectionsettext" |
	"sendmessage" |
	"setautoclose" |
	"setbkcolor" |
	"setbrandingimage" |
	"setcompress" |
	"setcompressor" |
	"setcurinsttype" |
	"setdatablockoptimize" |
	"setdatesave" |
	"setdetailsprint" |
	"setdetailsview" |
	"setfileattributes" |
	"setfont" |
	"setoutpath" |
	"setoverwrite" |
	"setpluginunload" |
	"setrebootflag" |
	"setshellvarcontext" |
	"setstaticbkcolor" |
	"setwindowlong" |
	"showinstdetails" |
	"showuninstdetails" |
	"showwindow" |
	"silentinstall" |
	"silentuninstall" |
	"sleep" |
	"spacetexts" |
	"strcpy" |
	"strlen" |
	"subcaption" |
	"uninstallbuttontext" |
	"uninstallcaption" |
	"uninstallicon" |
	"uninstallsubcaption" |
	"uninstalltext" |
	"uninstpage" |
	"unregdll" |
	"var" |
	"viaddversionkey" |
	"videscription" |
	"vicompanyname" |
	"vicomments" |
	"vilegalcopyrights" |
	"vilegaltrademarks" |
	"viproductname" |
	"viproductversion" |
	"windowicon" |
	"writeinistr" |
	"writeregbin" |
	"writeregdword" |
	"writeregexpandstr" |
	"writeregstr" |
	"writeuninstaller" |
	"xpstyle" |

	/* Flow control instructions */
	"abort" |
	"call" |
	"clearerrors" |
	"goto" |
	"ifabort" |
	"iferrors" |
	"iffileexists" |
	"ifrebootflag" |
	"intcmp" |
	"intcmpu" |
	"iswindow" |
	"messagebox" |
	"reboot" |
	"return" |
	"quit" |
	"seterrors" |
	"strcmp" |
	"strcmps"				{ addToken(Token.FUNCTION); }

	/* Compiler utility commands */
	"!addincludedir" |
	"!addplugindir" |
	"!define" |
	"!include" |
	"!cd" |
	"!echo" |
	"!error" |
	"!insertmacro" |
	"!packhdr" |
	"!system" |
	"!warning" |
	"!undef" |
	"!verbose" |

	/* Conditional compilation */
	"!ifdef" |
	"!ifndef" |
	"!if" |
	"!else" |
	"!endif" |
	"!macro" |
	"!macroend"				{ addToken(Token.RESERVED_WORD); }

	/* Global variables */
	"$0" |
	"$1" |
	"$2" |
	"$3" |
	"$4" |
	"$5" |
	"$6" |
	"$7" |
	"$8" |
	"$9" |
	"$INSTDIR" |
	"$OUTDIR" |
	"$CMDLINE" |
	"$LANGUAGE" |

	/* Local variables */
	("$R0"{Digit}) |

	/* Constants */
	"ARCHIVE" |
	"CENTER" |
	"CONTROL" |
	"CUR" |
	"EXT" |
	("F"{NonzeroDigit}) |
	("F1"{Digit}) |
	("F2"[0-4]) |
	"FILE_ATTRIBUTE_ARCHIVE" |
	"MB_ABORTRETRYIGNORE" |
	"RIGHT" |
	"RO" |
	"SET" |
	"SHIFT" |
	"SW_SHOWMAXIMIZED" |
	"SW_SHOWMINIMIZED" |
	"SW_SHOWNORMAL" |
	"a" |
	"admin" |
	"all" |
	"alwaysoff" |
	"auto" |
	"both" |
	"bottom" |
	"bzip2" |
	"checkbox" |
	"colored" |
	"components" |
	"current" |
	"custom" |
	"directory" |
	"force" |
	"hide" |
	"highest" |
	"ifnewer" |
	"instfiles" |
	"license" |
	"listonly" |
	"manual" |
	"nevershow" |
	"none" |
	"off" |
	"on" |
	"r" |
	"radiobuttons" |
	"show" |
	"silent" |
	"silentlog" |
	"smooth" |
	"textonly" |
	"top" |
	"try" |
	"uninstConfirm" |
	"user" |
	"w" |
	"zlib" |
	"$$" |
	"$DESKTOP" |
	"$EXEDIR" |
	"$HWNDPARENT" |
	"$PLUGINSDIR" |
	"$PROGRAMFILES" |
	"$QUICKLAUNCH" |
	"$SMPROGRAMS" |
	"$SMSTARTUP" |
	"$STARTMENU" |
	"$SYSDIR" |
	"$TEMP" |
	"$WINDIR" |
	"$\n" |
	"$\r" |
	"${NSISDIR}" |
	"ALT" |
	"END" |
	"FILE_ATTRIBUTE_HIDDEN" |
	"FILE_ATTRIBUTE_NORMAL" |
	"FILE_ATTRIBUTE_OFFLINE" |
	"FILE_ATTRIBUTE_READONLY" |
	"FILE_ATTRIBUTE_SYSTEM" |
	"FILE_ATTRIBUTE_TEMPORARY" |
	"HIDDEN" |
	"HKCC" |
	"HKCR" |
	"HKCU" |
	"HKDD" |
	"HKLM" |
	"HKPD" |
	"HKU" |
	"SHCTX" |
	"IDABORT" |
	"IDCANCEL" |
	"IDIGNORE" |
	"IDNO" |
	"IDOK" |
	"IDRETRY" |
	"IDYES" |
	"LEFT" |
	"MB_DEFBUTTON1" |
	"MB_DEFBUTTON2" |
	"MB_DEFBUTTON3" |
	"MB_DEFBUTTON4" |
	"MB_ICONEXCLAMATION" |
	"MB_ICONINFORMATION" |
	"MB_ICONQUESTION" |
	"MB_ICONSTOP" |
	"MB_OK" |
	"MB_OKCANCEL" |
	"MB_RETRYCANCEL" |
	"MB_RIGHT" |
	"MB_SETFOREGROUND" |
	"MB_TOPMOST" |
	"MB_YESNO" |
	"MB_YESNOCANCEL" |
	"NORMAL" |
	"OFFLINE" |
	"READONLY" |
	"SYSTEM" |
	"TEMPORARY"						{ addToken(Token.VARIABLE); }

	{LineTerminator}				{ addNullToken(); return firstToken; }

	/* Operators. */
	{Operator}					{ addToken(Token.OPERATOR); }

	{BooleanLiteral}				{ addToken(Token.LITERAL_BOOLEAN); }
	{Identifier}					{ addToken(Token.IDENTIFIER); }
	{Variable}						{ addToken(Token.VARIABLE); }

	{WhiteSpace}+					{ addToken(Token.WHITESPACE); }

	/* String/Character literals. */
	\"							{ start = zzMarkedPos-1; yybegin(STRING); }
	\'							{ start = zzMarkedPos-1; yybegin(CHAR_LITERAL); }
	\`							{ start = zzMarkedPos-1; yybegin(BACKTICKS); }

	/* Comment literals. */
	"/**/"						{ addToken(Token.COMMENT_MULTILINE); }
	{MLCBegin}					{ start = zzMarkedPos-2; yybegin(MLC); }
	{LineCommentBegin}			{ start = zzMarkedPos-1; yybegin(EOL_COMMENT); }

	/* Separators. */
	{Separator}					{ addToken(Token.SEPARATOR); }
	{Separator2}					{ addToken(Token.IDENTIFIER); }

	/* Numbers */
	{IntegerLiteral}				{ addToken(Token.LITERAL_NUMBER_DECIMAL_INT); }
	{HexLiteral}					{ addToken(Token.LITERAL_NUMBER_HEXADECIMAL); }
	{ErrorNumberFormat}				{ addToken(Token.ERROR_NUMBER_FORMAT); }

	/* Ended with a line not in a string or comment. */
	<<EOF>>						{ addNullToken(); return firstToken; }

	/* Catch any other (unhandled) characters and flag them as identifiers. */
	.							{ addToken(Token.IDENTIFIER); }

}


<STRING> {
	[^\n\\\$\"]+		{}
	\\.					{ /* Skip all escaped chars. */ }
	\\					{ /* Line ending in '\' => continue to next line. */
							addToken(start,zzStartRead, Token.LITERAL_STRING_DOUBLE_QUOTE);
							return firstToken;
						}
	{Variable}			{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.LITERAL_STRING_DOUBLE_QUOTE); addToken(temp,zzMarkedPos-1, Token.VARIABLE); start = zzMarkedPos; }
	{VariableStart}		{}
	\"					{ yybegin(YYINITIAL); addToken(start,zzStartRead, Token.LITERAL_STRING_DOUBLE_QUOTE); }
	\n |
	<<EOF>>				{ addToken(start,zzStartRead-1, Token.ERROR_STRING_DOUBLE); return firstToken; }
}


<CHAR_LITERAL> {
	[^\n\\\$\']+		{}
	\\.					{ /* Skip all escaped chars. */ }
	\\					{ /* Line ending in '\' => continue to next line. */
							addToken(start,zzStartRead, Token.LITERAL_CHAR);
							return firstToken;
						}
	{Variable}			{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.LITERAL_STRING_DOUBLE_QUOTE); addToken(temp,zzMarkedPos-1, Token.VARIABLE); start = zzMarkedPos; }
	{VariableStart}		{}
	\'					{ yybegin(YYINITIAL); addToken(start,zzStartRead, Token.LITERAL_CHAR); }
	\n |
	<<EOF>>				{ addToken(start,zzStartRead-1, Token.ERROR_CHAR); return firstToken; }
}


<BACKTICKS> {
	[^\n\\\$\`]+		{}
	\\.					{ /* Skip all escaped chars. */ }
	\\					{ /* Line ending in '\' => continue to next line. */
							addToken(start,zzStartRead, Token.LITERAL_BACKQUOTE);
							return firstToken;
						}
	{Variable}			{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.LITERAL_BACKQUOTE); addToken(temp,zzMarkedPos-1, Token.VARIABLE); start = zzMarkedPos; }
	{VariableStart}		{}
	\`					{ yybegin(YYINITIAL); addToken(start,zzStartRead, Token.LITERAL_BACKQUOTE); }
	\n |
	<<EOF>>				{ addToken(start,zzStartRead-1, Token.LITERAL_BACKQUOTE); return firstToken; }
}


<MLC> {
	[^hwf\n\*]+				{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_MULTILINE); start = zzMarkedPos; }
	[hwf]					{}

	\n						{ addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); return firstToken; }
	{MLCEnd}					{ yybegin(YYINITIAL); addToken(start,zzStartRead+1, Token.COMMENT_MULTILINE); }
	\*						{}
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_MULTILINE); return firstToken; }
}


<EOL_COMMENT> {
	[^hwf\n]+				{}
	{URL}					{ int temp=zzStartRead; addToken(start,zzStartRead-1, Token.COMMENT_EOL); addHyperlinkToken(temp,zzMarkedPos-1, Token.COMMENT_EOL); start = zzMarkedPos; }
	[hwf]					{}
	\n |
	<<EOF>>					{ addToken(start,zzStartRead-1, Token.COMMENT_EOL); addNullToken(); return firstToken; }
}
