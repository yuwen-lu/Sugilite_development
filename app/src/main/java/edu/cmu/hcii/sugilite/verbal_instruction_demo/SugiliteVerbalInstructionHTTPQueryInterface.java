package edu.cmu.hcii.sugilite.verbal_instruction_demo;

/**
 * @author toby
 * @date 12/10/17
 * @time 1:42 AM
 */
public interface SugiliteVerbalInstructionHTTPQueryInterface {
    void resultReceived(int responseCode, String result);
    void runOnMainThread(Runnable r);

}
