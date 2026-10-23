package com.bank.rules.engine;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Small rule-set documents for unit tests. */
final class Docs {

  private Docs() {}

  static ObjectNode json(String text) {
    try {
      return (ObjectNode) Values.mapper().readTree(text.replace('\'', '"'));
    } catch (Exception e) {
      throw new IllegalArgumentException(e);
    }
  }

  /** A minimal valid PRIORITY rule set: one DECIMAL input x, rule R1 fires when x > param limit. */
  static ObjectNode minimal() {
    return json(
        """
        {'format':'rule-set/v1','ruleSetId':'TEST.SET','domain':'TEST',
         'scope':{'lob':'LIFE','productClass':null},'version':'1.0.0',
         'effectiveFrom':'2026-11-01T00:00:00+05:30','effectiveTo':null,'classification':'OPERATIONAL',
         'inputs':[{'name':'x','type':'DECIMAL','required':true},
                   {'name':'d','type':'DATE','required':false}],
         'derive':[{'name':'twice','expr':{'op':'mul','args':[{'var':'x'},{'const':2}]}}],
         'decision':{'hitPolicy':'PRIORITY','outcomeRank':['BAD','OK'],'noMatchOutcome':'OK',
                     'missingInputOutcome':'BAD','missingConfigOutcome':'BAD',
                     'missingConfigReasonCode':'CFG'},
         'rules':[{'ruleId':'R1','priority':1,
                   'when':{'op':'gt','args':[{'var':'x'},{'param':'limit'}]},
                   'then':{'outcome':'BAD','reasonCode':'TOO_BIG','params':{'lim':{'param':'limit'}}}}],
         'parameters':{'limit':10},
         'reasonCodes':{'TOO_BIG':{'severity':'BLOCK','rmText':'x above @lim','customerText':'no'},
                        'CFG':{'severity':'BLOCK','rmText':'missing @missingParameters'},
                        'RULE_INPUT_MISSING':{'severity':'BLOCK','rmText':'need @missing'}},
         'testCases':[{'name':'t1','inputs':{'x':11},'expect':{'outcome':'BAD','reasonCodes':['TOO_BIG']}}]}
        """);
  }

  static RuleSet parse(JsonNode doc) {
    return RuleSetParser.parse(doc);
  }
}
