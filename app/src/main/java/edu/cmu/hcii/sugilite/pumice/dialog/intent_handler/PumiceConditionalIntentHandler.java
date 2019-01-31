package edu.cmu.hcii.sugilite.pumice.dialog.intent_handler;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import com.google.gson.Gson;

import java.util.Calendar;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.cmu.hcii.sugilite.pumice.communication.PumiceInstructionPacket;
import edu.cmu.hcii.sugilite.pumice.communication.PumiceSemanticParsingResultPacket;
import edu.cmu.hcii.sugilite.pumice.dialog.PumiceDialogManager;
import edu.cmu.hcii.sugilite.pumice.kb.PumiceBooleanExpKnowledge;
import edu.cmu.hcii.sugilite.ui.ScriptDetailActivity;
import edu.cmu.hcii.sugilite.model.block.SugiliteConditionBlock;
import edu.cmu.hcii.sugilite.model.block.SugiliteBlock;
import edu.cmu.hcii.sugilite.SugiliteData;
import edu.cmu.hcii.sugilite.verbal_instruction_demo.server_comm.SugiliteVerbalInstructionHTTPQueryInterface;

public class PumiceConditionalIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface  {
/*import static edu.cmu.hcii.sugilite.pumice.dialog.intent_handler.PumiceUtteranceIntentHandler.PumiceIntent.FIX_SCOPE;

public class PumiceConditionalIntentHandler implements PumiceUtteranceIntentHandler, SugiliteVerbalInstructionHTTPQueryInterface {
    private transient ScriptDetailActivity sourceActivity;
    private PumiceDialogManager dialogManager;*/
    private transient Context context;
    private transient PumiceDialogManager pumiceDialogManager;
    private ExecutorService es;
    Calendar calendar;
    private PumiceIntent lastIntent; //store last PumiceIntent
    private PumiceDialogManager.PumiceUtterance lastUtterance; //store last utterance
    private String s; //string to hold "true" or "false" for message to user
    private boolean moving; //true if currently moving a step around in the script
    private SugiliteBlock storedBlock;
    private SugiliteConditionBlock originalBlock;
    ////
    private boolean justAddElse = false; //remember to add else statement to script
    public PumiceConditionalIntentHandler(PumiceDialogManager pdm, Context context){
        this.context = context;
    /*public PumiceConditionalIntentHandler(PumiceDialogManager dialogManager, ScriptDetailActivity sourceActivity){
        this.dialogManager = dialogManager;
        this.sourceActivity = sourceActivity;
        this.context = sourceActivity;*/
        this.calendar = Calendar.getInstance();
        this.es = Executors.newCachedThreadPool();
        this.pumiceDialogManager = pdm;
    }

    @Override
    public void setContext(Activity context) {
        this.context = context;
    }

    @Override
    public PumiceIntent detectIntentFromUtterance(PumiceDialogManager.PumiceUtterance utterance) {
        String text = utterance.getContent().toLowerCase();
        if(lastIntent == PumiceIntent.TELL_ELSE) {
            return PumiceIntent.ADD_TELL_ELSE;
        }
        if((lastIntent == PumiceIntent.ADD_TELL_ELSE || justAddElse) && text.contains("yes")) {
            return PumiceIntent.SCRIPT_ADD_TELL_ELSE;
        }
        if(lastIntent == PumiceIntent.ADD_TELL_IF && text.contains("yes")) {
            return PumiceIntent.SCRIPT_ADD_TELL_IF;
        }
        if(text.contains("explain") && lastIntent == PumiceIntent.TELL_IF) {
            return PumiceIntent.ADD_TELL_IF;
        }
        if(lastIntent == PumiceIntent.CHECKING_LOC && lastUtterance.getContent().contains("no")) {
            return PumiceIntent.TELL_IF;
        }
        if(lastIntent == PumiceIntent.CHECKING_LOC0 && lastUtterance.getContent().contains("yes")) {
            return PumiceIntent.CHECKING_LOC;
        }
        if((lastIntent == PumiceIntent.FIX_SCRIPT2 && !moving)) {
            return FIX_SCOPE;
        }
        if(lastIntent == PumiceIntent.FIX_SCRIPT || (moving && text.contains("yes") && lastIntent == PumiceIntent.MOVE_STEP)) {
            return PumiceIntent.FIX_SCRIPT2;
        }
        if(lastIntent == PumiceIntent.RUN_THROUGH2 && (utterance.getContent().contains("pause") || utterance.getContent().contains("no"))) {
            return PumiceIntent.FIX_SCRIPT;
        }
        if(lastIntent == PumiceIntent.RUN_THROUGH && lastUtterance.getContent().contains("yes")) {
            return PumiceIntent.RUN_THROUGH2;
        }
        if(lastIntent == PumiceIntent.ADD_CONDITIONAL_2 && text.contains("yes")) {
            return PumiceIntent.CHECKING_LOC0;
        }
        if(text.contains("explain") && lastIntent == PumiceIntent.ADD_ELSE) {
            return PumiceIntent.TELL_ELSE;
        }
        if(lastIntent == PumiceIntent.GET_SCOPE) {
            return PumiceIntent.ADD_ELSE;
        }
        if(lastUtterance != null && lastUtterance.getContent().contains("yes") && lastIntent == PumiceIntent.CHECKING_LOC) {
            return PumiceIntent.GET_SCOPE;
        }
        if((text.contains("before") || text.contains("after")) && lastIntent == PumiceIntent.ADD_TO_SCRIPT) {
            return PumiceIntent.ADD_CONDITIONAL_2;
        }
        if(text.contains("if") || text.contains("when") || text.contains("unless")) { //will need more
            return PumiceIntent.ADD_CONDITIONAL;
        }
        if(((text.contains("yes") || text.contains("no")) && (lastIntent == PumiceIntent.SCRIPT_ADD_TELL_IF || lastIntent == PumiceIntent.SCRIPT_ADD_TELL_ELSE)) || (lastIntent == PumiceIntent.ADD_ELSE && lastUtterance.getContent().contains("no"))) {
            return PumiceIntent.RUN_THROUGH;
        }
        if(((text.contains("yes") || text.contains("no")) && (lastIntent == PumiceIntent.ADD_TO_SCRIPT) || (lastIntent == PumiceIntent.MOVE_STEP) && text.contains("yes"))) {
            return PumiceIntent.CHECKING_LOC0;
        }
        if(lastIntent == PumiceIntent.CHECKING_LOC0 || moving || ((text.contains("no") && lastIntent == PumiceIntent.ADD_CONDITIONAL_2) || ((lastIntent == PumiceIntent.CHECKING_LOC2) && lastUtterance.getContent().contains("no"))) || (lastIntent == PumiceIntent.MOVE_STEP && !lastUtterance.getContent().matches(".*\\d+.*")) || (lastIntent == PumiceIntent.MOVE_STEP && text.contains("no"))) {
            return PumiceIntent.MOVE_STEP;
        }
        if((text.contains("yes")) || text.contains("no")) {
            return PumiceIntent.ADD_TO_SCRIPT;
        }
        else {
            return PumiceIntent.USER_INIT_INSTRUCTION;
        }
    }

    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {
        //placeholder for compiling
    }

    /*
    @Override
    public void handleIntentWithUtterance(PumiceDialogManager dialogManager, PumiceIntent pumiceIntent, PumiceDialogManager.PumiceUtterance utterance) {

        switch(pumiceIntent) {
            case FIX_SCOPE:
                lastIntent = FIX_SCOPE;
                lastUtterance = utterance;
                sourceActivity.fixScope(utterance.getContent(),s);
                if(s.equals("true")) {
                    originalBlock.setElseBlock(storedBlock);
                }
                else {
                    originalBlock.setThenBlock(storedBlock);
                }
                sourceActivity.loadOperationList();
                break;
            case FIX_SCRIPT2:
                lastIntent = PumiceIntent.FIX_SCRIPT2;
                lastUtterance = utterance;
                if((utterance.getContent().contains(s) || utterance.getContent().contains("step"))) {
                    moving = false;
                    if(s.equals("true")) {
                        storedBlock = ((SugiliteConditionBlock) dialogManager.conditionBlock).getThenBlock();
                        originalBlock.setElseBlock(null);
                    }
                    else {
                        storedBlock = ((SugiliteConditionBlock) dialogManager.conditionBlock).getElseBlock();
                        originalBlock.setThenBlock(null);
                    }
                    sourceActivity.loadOperationList();
                    dialogManager.sendAgentMessage("Ok, please tell me the last step shown that should happen only if the check is " + s, true, true);
                }
                /*else if(utterance.getContent().contains("happens") || utterance.getContent().contains("true") || utterance.getContent().contains("false")) {
                    if(s.equals("true") && ((SugiliteConditionBlock) dialogManager.tResult).getIfBlock().getNextBlockCond() == null) {
                        dialogManager.sendAgentMessage("Ok, please tell me what I should actually do if " + dialogManager.check + "is true.", true, true);
                    }
                    else if(s.equals("false") && ((SugiliteConditionBlock) dialogManager.tResult).getElseBlock().getNextBlockCond() == null) {
                        dialogManager.sendAgentMessage("Ok, please tell me what I should actually do if " + dialogManager.check + "is false.", true, true);
                    }
                    else {
                        dialogManager.sendAgentMessage("Ok, please tell me which step went wrong.", true, true);
                    }
                }*/
    /*
                else {
                    moving = true;
                    dialogManager.sendAgentMessage("Ok, please tell me after what step the check should happen.", true, true);
                }
                break;
            case RUN_THROUGH2:
                lastIntent = PumiceIntent.RUN_THROUGH2;
                lastUtterance = utterance;
                SugiliteData sugiliteData2 = sourceActivity.getSugiliteData();
                sugiliteData2.testRun = true;
                if(utterance.getContent().contains("true")) {
                    sugiliteData2.testing = true;
                }
                sourceActivity.testRun();
                break;
            case FIX_SCRIPT:
                lastIntent = PumiceIntent.FIX_SCRIPT;
                s = "false";
                if(lastUtterance.getContent().contains("true")) {
                    s = "true";
                }
                lastUtterance = utterance;
		
                //SugiliteData sugiliteData = ((ScriptDetailActivity) dialogManager.context).getSugiliteData();
                //sugiliteData.statusIconManager.pauseTestRun(((ScriptDetailActivity) dialogManager.context));
                //dialogManager.sendAgentMessage("I understand there is a problem. Is there a problem with when the check happens or with what happens when the check is " + s + "?", true, true);
                SugiliteData sugiliteData = sourceActivity.getSugiliteData();
                sugiliteData.statusIconManager.pauseTestRun(sourceActivity);
                dialogManager.sendAgentMessage("I understand there is a problem. Is there a problem with when the check happens or with what happens when the check is" + s + "?", true, true);
                break;
            case SCRIPT_ADD_TELL_ELSE:
                justAddElse = false;
                lastIntent = PumiceIntent.SCRIPT_ADD_TELL_ELSE;
                lastUtterance = utterance;
                SugiliteBlock block = ((SugiliteConditionBlock) dialogManager.tResult).getThenBlock();
                ((SugiliteConditionBlock) dialogManager.conditionBlock).setElseBlock(block);
                block.setPreviousBlock(null);
                block.setParentBlock(dialogManager.conditionBlock);
                block.setNextBlock(null);
                sourceActivity.loadOperationList();
                dialogManager.sendAgentMessage("Would you like to run through the task to make sure the check works correctly?", true, true);
                dialogManager.addElse = false;
                break;
            case ADD_TELL_ELSE:
                lastIntent = PumiceIntent.ADD_TELL_ELSE;
                justAddElse = true;
                lastUtterance = utterance;
                dialogManager.addElse = true;
                dialogManager.conditionBlock = dialogManager.tResult;
                PumiceInstructionPacket pumiceInstructionPacket4 = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.DEFINE_VALUE_EXP, calendar.getTimeInMillis(), utterance.getContent(), "");//"if it is hot" +
                dialogManager.sendAgentMessage("Let's make sure I understood what you said...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket4.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket4, this);
                } catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(pumiceInstructionPacket4.toString());
                break;
            case TELL_ELSE:
                lastIntent = PumiceIntent.TELL_ELSE;
                lastUtterance = utterance;
                dialogManager.sendAgentMessage("Ok, what should I do if" + dialogManager.check + "is false?",true,true);
                break;
            case ADD_ELSE:
                lastIntent = PumiceIntent.ADD_ELSE;
                lastUtterance = utterance;
                if(utterance.getContent().contains("yes")) {
                    dialogManager.sendAgentMessage("Ok, I need you to tell me what to do if" + dialogManager.check + "is false. Would you like to explain or demonstrate what to do?", true, true);
                }
                else {
                    dialogManager.sendAgentMessage("Ok, would you like to run through the task to make sure the check and steps happen correctly?", true, true);
                }
                break;
            case GET_SCOPE:
                lastIntent = PumiceIntent.GET_SCOPE;
                lastUtterance = utterance;
                sourceActivity.getScope(utterance.getContent());
                dialogManager.sendAgentMessage("Ok, would you like to do something if " + dialogManager.check + "is false?",true,true);
                break;
            case ADD_TO_SCRIPT:
                lastIntent = PumiceIntent.ADD_TO_SCRIPT;
                lastUtterance = utterance;
                if(utterance.getContent().contains("yes")) {
                    //((ScriptDetailActivity) dialogManager.context).determineConditionalLoc(dialogManager.tResult);
                    dialogManager.setPcih(this);
                    sourceActivity.determineConditionalLoc(dialogManager.tResult);
                }
                else {
                    //
                }
                break;
            case ADD_CONDITIONAL_2:
                lastIntent = PumiceIntent.ADD_CONDITIONAL_2;
                lastUtterance = utterance;
                boolean after = false;
                if(utterance.getContent().contains("after")) {
                    after = true;
                }
                sourceActivity.determineConditionalLoc2(after);
                break;
            case ADD_CONDITIONAL:
                lastIntent = PumiceIntent.ADD_CONDITIONAL;
                lastUtterance = utterance;
                String[] removeIf = utterance.getContent().split(" ",3);
                String toParse = removeIf[2];
                //PumiceInstructionPacket pumiceInstructionPacket2 = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), "BOOL_EXP_INSTRUCTION", calendar.getTimeInMillis(), toParse, null);
                PumiceInstructionPacket pumiceInstructionPacket2 = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.DEFINE_BOOL_EXP, calendar.getTimeInMillis(), toParse, "");
                dialogManager.sendAgentMessage("Let's make sure I understood what you said...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket2.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket2, this);
                } catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(pumiceInstructionPacket2.toString());
                break;
            case CHECKING_LOC0:
                lastIntent = PumiceIntent.CHECKING_LOC0;
                lastUtterance = utterance;
                originalBlock = ((SugiliteConditionBlock) dialogManager.tResult);
                if(utterance.getContent().contains("yes")) {
                    dialogManager.sendAgentMessage("Ok, are the steps that you want to happen if the check is true already part of the script?", true, true);
                }
                else {
                    dialogManager.sendAgentMessage("Ok, after which step should I perform the check?",true,true);
                }
                break;
            case CHECKING_LOC:
                lastIntent = PumiceIntent.CHECKING_LOC;
                lastUtterance = utterance;
                if(utterance.getContent().contains("yes")) {
                    dialogManager.sendAgentMessage("Ok, please tell me the last step that should happen only if" + dialogManager.check + "is true.", true,true);
                }
                else {
                    dialogManager.sendAgentMessage("Ok, I need you to tell me what to do if" + dialogManager.check + "is true. Would you like to explain or demonstrate what to do?", true, true);
                }
                break;
            case TELL_IF:
                lastIntent = PumiceIntent.TELL_IF;
                lastUtterance = utterance;
                dialogManager.sendAgentMessage("Ok, what should I do if" + dialogManager.check + "is true?",true,true);
                break;
            case ADD_TELL_IF:
                lastIntent = PumiceIntent.ADD_TELL_IF;
                lastUtterance = utterance;
                dialogManager.addElse = true;
                dialogManager.conditionBlock = dialogManager.tResult;
                //PumiceInstructionPacket pumiceInstructionPacket5 = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.USER_INIT_INSTRUCTION, calendar.getTimeInMillis(), "if it is hot" + utterance.getContent(), "ROOT");
                PumiceInstructionPacket pumiceInstructionPacket5 = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.DEFINE_BOOL_EXP, calendar.getTimeInMillis(), utterance.getContent(), "");//"if it is hot" +
                    dialogManager.sendAgentMessage("Let's make sure I understood what you said...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket5.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket5, this);
                } catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(pumiceInstructionPacket5.toString());
                break;
            case SCRIPT_ADD_TELL_IF:
                lastIntent = PumiceIntent.SCRIPT_ADD_TELL_IF;
                lastUtterance = utterance;
                SugiliteBlock block2 = ((SugiliteConditionBlock) dialogManager.tResult).getThenBlock();
                ((SugiliteConditionBlock) dialogManager.conditionBlock).setThenBlock(block2);
                block2.setPreviousBlock(null);
                block2.setParentBlock(dialogManager.conditionBlock);
                block2.setNextBlock(null);
                sourceActivity.loadOperationList();
                dialogManager.sendAgentMessage("Would you like to run through the task to make sure the check works correctly?", true, true);
                dialogManager.addElse = false;
                break;
            case RUN_THROUGH:
                lastIntent = PumiceIntent.RUN_THROUGH;
                lastUtterance = utterance;
                if(utterance.getContent().contains("yes")) {
                    dialogManager.sendAgentMessage("Ok, would you like to test when the check is true or when it's false?",true,true);
                }
                else {
                    dialogManager.sendAgentMessage("Ok, you're all set.",true,false);
                    ((ScriptDetailActivity) dialogManager.context).getRidOfScope();
                }
                break;
            case MOVE_STEP:
                lastIntent = PumiceIntent.MOVE_STEP;
                lastUtterance = utterance;
                if(!utterance.getContent().matches(".*\\d+.*")) {//need to account for if say number that isn't step so check if says "step" or just a number should mean step # was given.
                    dialogManager.sendAgentMessage("Please say the number of the step after which the check should happen.",true,true);
                }
                else {
                    sourceActivity.moveStep(utterance.getContent());
                    dialogManager.sendAgentMessage("Ok, does it look like the check happens at the right time?",true,true);
                }
                break;
            case USER_INIT_INSTRUCTION:
                lastIntent = PumiceIntent.USER_INIT_INSTRUCTION;
                lastUtterance = utterance;
                dialogManager.sendAgentMessage("I have received your instruction: " + utterance.getContent(), true, false);
                //PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.USER_INIT_INSTRUCTION, calendar.getTimeInMillis(), utterance.getContent(), "ROOT");
                PumiceInstructionPacket pumiceInstructionPacket = new PumiceInstructionPacket(dialogManager.getPumiceKnowledgeManager(), PumiceIntent.USER_INIT_INSTRUCTION, calendar.getTimeInMillis(), utterance.getContent(), "");
                dialogManager.sendAgentMessage("Sending out the server query below...", true, false);
                dialogManager.sendAgentMessage(pumiceInstructionPacket.toString(), false, false);
                try {
                    dialogManager.getHttpQueryManager().sendPumiceInstructionPacketOnASeparateThread(pumiceInstructionPacket, this);
                } catch (Exception e){
                    e.printStackTrace();
                }
                System.out.println(pumiceInstructionPacket.toString());
                break;
            default:
                dialogManager.sendAgentMessage("I don't understand this intent", true, false);
                break;
        }
    }
    */


    /*@Override
    public void handleServerResponse(PumiceDialogManager dialogManager, int responseCode, String result) {
        //TODO: handle server response
        //notify the thread for resolving unknown bool exp that the intent has been fulfilled


    }*/

    @Override
    public void resultReceived(int responseCode, String result) {
        //TODO: handle server response from the semantic parsing server
        Gson gson = new Gson();
        try {
            PumiceSemanticParsingResultPacket resultPacket = gson.fromJson(result, PumiceSemanticParsingResultPacket.class);
        //TODO: handle server response from the semantic parsing server
            if (resultPacket.utteranceType != null) {
                switch (PumiceUtteranceIntentHandler.PumiceIntent.valueOf(resultPacket.utteranceType)) {
                    case USER_INIT_INSTRUCTION:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            if (topResult.formula != null) {
                                pumiceDialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                pumiceDialogManager.sendAgentMessage(topResult.formula, false, false);
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
					//pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula);
                                        dialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula, resultPacket.userUtterance);
                                    }
                                };
                                //do the parse on a new thread so it doesn't block the conversational I/O
                                es.submit(r);
                            }
                        }
                        break;
                    case BOOL_EXP_INSTRUCTION:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            if (topResult.formula != null) {
                                pumiceDialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                pumiceDialogManager.sendAgentMessage(topResult.formula, false, false);
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {

					    //pumiceDialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction("(call if " + topResult.formula + "(call click (hasText 'hi')))");
                                        dialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula, resultPacket.userUtterance);
                                    }
                                };
                                //do the parse on a new thread so it doesn't block the conversational I/O
                                es.submit(r);
                            }
                        }
                        break;
                    case DEFINE_VALUE_EXP:
                        if (resultPacket.queries != null && resultPacket.queries.size() > 0) {
                            PumiceSemanticParsingResultPacket.QueryGroundingPair topResult = resultPacket.queries.get(0);
                            if (topResult.formula != null) {
                                dialogManager.sendAgentMessage("Received the parsing result from the server: ", true, false);
                                dialogManager.sendAgentMessage(topResult.formula, false, false);
                                Runnable r = new Runnable() {
                                    @Override
                                    public void run() {
                                        dialogManager.getPumiceInitInstructionParsingHandler().parseFromNewInitInstruction(topResult.formula, resultPacket.userUtterance);
                                    }
                                };
                                //do the parse on a new thread so it doesn't block the conversational I/O
                                es.submit(r);
                            }
                        }
                        break;
                    default:
                        pumiceDialogManager.sendAgentMessage("Can't read from the server response", true, false);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void runOnMainThread(Runnable r) {
	//pumiceDialogManager.runOnMainThread(r);
        sourceActivity.runOnUiThread(r);
    }
}
