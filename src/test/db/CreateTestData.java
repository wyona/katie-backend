import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.UUID;

/**
 *
 */
public class CreateTestData {

    /**
     *
     */
    public static void main(String[] args) {
        try {
            createAfterMigrateSQL(10000);
        } catch(Exception e) {
            System.err.println(e.getMessage());
        }
    }

    /**
     * @param size Number of inserts
     */
    private static void createAfterMigrateSQL(int size) throws Exception {
        System.out.println("Create SQL script afterMigrate.sql ...");

        FileOutputStream out = new FileOutputStream("afterMigrate.sql");
        OutputStreamWriter writer = new OutputStreamWriter(out);

        writer.write("/* Insert resubmitted questions to test scalability */");
        writer.write("\n");
        for (int i = 0; i < size; i++) {
            String uuid = UUID.randomUUID().toString();
            long epoch = new Date().getTime();
            String insert = "insert into RESUBMITTED_QUESTION (UUID, DOMAIN_ID, QUESTION, EMAIL, FCM_TOKEN, SLACK_CHANNEL_ID, ANSWER_LINK_TYPE, STATUS, TRAINED, REMOTE_ADDRESS, TIMESTAMP_RESUBMITTED, ANSWER, ANSWER_CLIENT_SIDE_ENCRYPTED_ALGORITHM, OWNERSHIP, RESPONDENT, RATING, FEEDBACK)  values ('" + uuid + "', 'ROOT', 'What is the complete list of Arsène Lupin books by Maurice Leblanc?', 'michael.wechner@wyona.com', null, null, null, 'answer-pending', false, '172.17.0.1', " + epoch + ", null, null, 'iam:public', 'TODO_1', null, null);";
            //System.out.println(insert);
            writer.write(insert);
            writer.write("\n");
        }
        writer.close();
        out.close();
    }
}
