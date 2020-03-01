parser grammar HoshParser;

options {
	tokenVocab=HoshLexer;
}

program
	: ( stmt )* EOF
	;

stmt
	: sequence terminator?
	| terminator
	;

sequence
	: pipeline terminator sequence
	| pipeline
	;

pipeline
	: command PIPE stmt
	| command PIPE // will be rejected by compiler
	| command
	;

command
	: wrapped
	| simple
	| lambda
	;

wrapped
	: invocation OPEN_CURLY stmt CLOSE_CURLY
	| invocation OPEN_CURLY CLOSE_CURLY // will be rejected by compiler
	| wrapped CLOSE_CURLY // will be rejected by compiler
	;

simple
	: invocation
	;

lambda
	: OPEN_CURLY ID ARROW stmt CLOSE_CURLY
	;

// by now compiler requires command (ID) to be statically defined
// later should be possible to compile line by line, performing variable expansion before compiling
invocation
	: ID ( expression )*
	;

expression
	: ID
	| expansion
	| string
	;

string
	: sqstring
	| dqstring
	;

sqstring
	: OPEN_SQUOTE SQUOTE_TEXT* CLOSE_SQUOTE
	;

dqstring
	: OPEN_DQUOTE (dqpart)* CLOSE_DQUOTE
	;

dqpart
	: DQUOTE_TEXT
	| DQUOTE_VARIABLE
	| DQUOTE_VARIABLE_OR_FALLBACK
	;

expansion
	: VARIABLE
	| VARIABLE_OR_FALLBACK
	;

terminator
	: SEMICOLON
	| NEWLINE
	;
