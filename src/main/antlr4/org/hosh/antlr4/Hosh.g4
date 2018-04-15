grammar Hosh;

program: stmt+ ;
stmt: ID ID*;
ID : [a-zA-Z0-9-]+ ;
WS : [ \t]+ -> skip ;
