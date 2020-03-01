lexer grammar HoshLexer;

PIPE
	: '|'
	;

OPEN_CURLY
	: '{'
	;

CLOSE_CURLY
	: '}'
	;

ARROW
	: '->'
	;

SEMICOLON
	: ';'
	;

OPEN_SQUOTE
	: '\'' -> pushMode(LITERAL_STRING)
	;

OPEN_DQUOTE
	: '"' -> pushMode(STRING)
	;

ID
	: I+
	;

VARIABLE
	: '$' '{' V+ '}'
	;

VARIABLE_OR_FALLBACK
	: '$' '{' V+ '!' I+ '}'
	;

fragment I : LETTER | DIGIT | ':' | '_' | '-' | '.' | '/' | '\\' | '~' | '+' | '*' | '=' | '?' | '<' | '>' | '(' | ')' | ',';

fragment V : LETTER | '_' | '-' ;

fragment LETTER: LOWER | UPPER;

fragment LOWER: 'a'..'z';

fragment UPPER: 'A'..'Z';

fragment DIGIT: '0'..'9';

COMMENT
	:  '#' ~('\r' | '\n')* -> skip
	;

WS
	: [ \t\n\r]+ -> skip
	;

mode LITERAL_STRING;

CLOSE_SQUOTE: '\'' -> popMode;

SQUOTE_TEXT
   	: ~[']+
	;

mode STRING;

CLOSE_DQUOTE: '"' -> popMode;

DQUOTE_TEXT
	: ~('$' | '"')+
	;

DQUOTE_VARIABLE
	: '${' V+ CLOSE_CURLY
	;

DQUOTE_VARIABLE_OR_FALLBACK
	: '${' V+ '!' I+ CLOSE_CURLY
	;

