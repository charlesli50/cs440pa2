# export JAVA_HOME=`/usr/libexec/java_home -v 1.8.0_432`

javac -cp "./lib/*:." @pas-stealth.srcs   

filesToTest=("OneUnitSmallMaze" "TwoUnitSmallMaze" "BigMaze")
totalTimeTaken=0

for mze in "${filesToTest[@]}"; do
    echo "Running $mze..."
    startTime=$(date +%s)

    winCount=$(for i in {1..300}; do
            java -cp "./lib/*:." edu.cwru.sepia.Main2 data/pas/stealth/$mze.xml
        done | grep -c "you win!") 

    endTime=$(date +%s)
    winRate=$(awk "BEGIN {printf \"%.2f\", $winCount/300 * 100}")
    timeTaken=$((endTime - startTime))
    
    echo "Win Rate: $winRate% ($winCount/300)"
    echo "Time Taken: $timeTaken seconds"
    echo "--------------------------------"
    totalTimeTaken=$((totalTimeTaken + timeTaken))
done

echo "Total Time Taken: $totalTimeTaken seconds"