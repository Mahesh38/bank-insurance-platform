package com.bank.rules.engine;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/** The closed operator set (ADR-024 section 3), including its null semantics. */
class OperatorTest {

  private static final String INPUTS =
      "{'n':12.5,'i':4,'s':'abc','b':true,'d':'2026-01-01','d2':'2026-03-01T10:00:00+05:30',"
          + "'e':'A','l':['A','B'],'z':0}";

  private static EvaluationResult run(String expr, String inputs) {
    ObjectNode doc =
        Docs.json(
            """
            {'format':'rule-set/v1','ruleSetId':'OPS','domain':'T','scope':{},'version':'1.0.0',
             'effectiveFrom':'2026-01-01T00:00:00Z','classification':'OPERATIONAL','scale':4,
             'inputs':[{'name':'n','type':'DECIMAL'},{'name':'i','type':'INTEGER'},
               {'name':'s','type':'STRING'},{'name':'b','type':'BOOLEAN'},{'name':'d','type':'DATE'},
               {'name':'d2','type':'DATE'},{'name':'e','type':'ENUM','values':['A','B']},
               {'name':'l','type':'LIST'},{'name':'z','type':'DECIMAL'}],
             'tables':{'rng':{'type':'RANGE','rows':[{'from':0,'to':10,'value':'low'},{'from':10,'to':null,'value':'high'}]},
                       'ex':{'type':'EXACT','rows':[{'key':'A','value':1},{'key':'B','value':2}]}},
             'decision':{'hitPolicy':'COLLECT','noMatchOutcome':'NONE','missingInputOutcome':'MISSING',
                         'missingConfigOutcome':'GAP'},
             'rules':[{'ruleId':'R','priority':1,'when':{'const':false},'then':{'outcome':'X'}}],
             'parameters':{'p':3,'nested':{'k':5},'nul':null,'list':['A']},
             'reasonCodes':{},
             'testCases':[{'name':'t','inputs':{},'expect':{}}]}
            """);
    ((com.fasterxml.jackson.databind.node.ArrayNode) doc.withArray("derive"))
        .addObject()
        .put("name", "out")
        .set("expr", Docs.json(expr));
    return RuleEngine.evaluate(Docs.parse(doc), Docs.json(inputs));
  }

  private static Object eval(String expr) {
    return run(expr, INPUTS).derived().get("out");
  }

  private static void num(String expr, String expected) {
    assertThat((BigDecimal) eval(expr)).isEqualByComparingTo(expected);
  }

  @Test
  void logic() {
    assertThat(eval("{'op':'all','args':[{'var':'b'},{'const':true}]}")).isEqualTo(true);
    assertThat(eval("{'op':'all','args':[{'var':'b'},{'const':null}]}")).isEqualTo(false);
    assertThat(eval("{'op':'any','args':[{'const':false},{'var':'b'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'any','args':[{'const':false}]}")).isEqualTo(false);
    assertThat(eval("{'op':'not','args':[{'var':'b'}]}")).isEqualTo(false);
  }

  @Test
  void comparisons() {
    assertThat(eval("{'op':'eq','args':[{'var':'i'},{'const':4.0}]}")).isEqualTo(true);
    assertThat(eval("{'op':'eq','args':[{'var':'s'},{'const':'abc'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'eq','args':[{'var':'d'},{'const':'2026-01-01'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'ne','args':[{'var':'s'},{'const':'x'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'ne','args':[{'const':null},{'const':'x'}]}")).isEqualTo(false);
    assertThat(eval("{'op':'gt','args':[{'var':'n'},{'param':'p'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'gte','args':[{'var':'i'},{'const':4}]}")).isEqualTo(true);
    assertThat(eval("{'op':'lt','args':[{'var':'d'},{'var':'d2'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'lte','args':[{'var':'s'},{'const':'abd'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'gt','args':[{'var':'s'},{'const':1}]}")).isEqualTo(false);
    assertThat(eval("{'op':'in','args':[{'var':'e'},{'var':'l'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'in','args':[{'var':'e'},{'const':'notalist'}]}")).isEqualTo(false);
    assertThat(eval("{'op':'notIn','args':[{'const':'C'},{'param':'list'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'notIn','args':[{'const':null},{'param':'list'}]}")).isEqualTo(false);
    assertThat(eval("{'op':'between','args':[{'var':'i'},{'const':4},{'const':5}]}"))
        .isEqualTo(true);
    assertThat(eval("{'op':'between','args':[{'var':'i'},{'const':5},{'const':6}]}"))
        .isEqualTo(false);
    assertThat(eval("{'op':'isNull','args':[{'param':'nul'}]}")).isEqualTo(true);
    assertThat(eval("{'op':'isPresent','args':[{'param':'nested.k'}]}")).isEqualTo(true);
  }

  @Test
  void arithmeticIsBigDecimalHalfUpAtDeclaredScale() {
    num("{'op':'add','args':[{'var':'n'},{'var':'i'},{'const':0.5}]}", "17");
    num("{'op':'sub','args':[{'var':'n'},{'var':'i'}]}", "8.5");
    num("{'op':'mul','args':[{'var':'n'},{'param':'nested.k'}]}", "62.5");
    num("{'op':'div','args':[{'const':2},{'const':3}]}", "0.6667");
    num("{'op':'min','args':[{'var':'n'},{'var':'i'}]}", "4");
    num("{'op':'max','args':[{'var':'n'},{'var':'i'}]}", "12.5");
    num("{'op':'abs','args':[{'const':-3}]}", "3");
    num("{'op':'roundUpTo','args':[{'const':1234567},{'const':100000}]}", "1300000");
    num("{'op':'roundUpTo','args':[{'const':1200000},{'const':100000}]}", "1200000");
    assertThat(eval("{'op':'div','args':[{'var':'n'},{'var':'z'}]}")).isNull();
    assertThat(eval("{'op':'add','args':[{'var':'n'},{'const':null}]}")).isNull();
    assertThat(eval("{'op':'abs','args':[{'const':null}]}")).isNull();
    assertThat(eval("{'op':'roundUpTo','args':[{'var':'n'},{'const':0}]}")).isNull();
  }

  @Test
  void domainFunctions() {
    assertThat(eval("{'op':'lookup','args':[{'const':'rng'},{'var':'n'}]}")).isEqualTo("high");
    assertThat(eval("{'op':'lookup','args':[{'const':'rng'},{'var':'i'}]}")).isEqualTo("low");
    assertThat(eval("{'op':'lookup','args':[{'const':'rng'},{'const':-1}]}")).isNull();
    assertThat(eval("{'op':'lookup','args':[{'const':'rng'},{'const':null}]}")).isNull();
    num("{'op':'lookup','args':[{'const':'ex'},{'var':'e'}]}", "1");
    assertThat(eval("{'op':'lookup','args':[{'const':'ex'},{'const':'Q'}]}")).isNull();
    num("{'op':'daysBetween','args':[{'var':'d'},{'var':'d2'}]}", "59");
    assertThat(eval("{'op':'daysBetween','args':[{'var':'d'},{'const':'garbage'}]}")).isNull();
    num("{'op':'ageAt','args':[{'const':'1988-03-02'},{'var':'d2'}]}", "37");
    assertThat(eval("{'op':'ageAt','args':[{'const':null},{'var':'d2'}]}")).isNull();
    num("{'op':'coalesce','args':[{'param':'nul'},{'const':7}]}", "7");
    assertThat(eval("{'op':'coalesce','args':[{'param':'nul'}]}")).isNull();
  }

  @Test
  void anUnsetParameterInADerivationIsAConfigurationGapForTheWholeSet() {
    EvaluationResult r = run("{'op':'add','args':[{'param':'nul'},{'const':1}]}", INPUTS);
    assertThat(r.outcome()).isEqualTo("GAP");
    assertThat(r.reasonCodes()).containsExactly(RuleEngine.RULE_CONFIG_MISSING);
    assertThat(r.fired().get(0).configMissing()).containsExactly("nul");
  }
}
