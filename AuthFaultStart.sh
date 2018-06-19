screen -dmS new_screen sh
screen -S new_screen -X stuff "/users/mcrompto/jre-10.0.1/bin/java -cp /users/mcrompto/CRARiskExperiment2/bin edu.ucdavis.cs.cra.TestAuth $1 true\n"
