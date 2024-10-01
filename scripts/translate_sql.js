const fs = require('fs');

// Function to replace placeholders with parameter values
function translateSQL(sqlFile) {
    // Read the SQL file content
    let sqlTemplate = fs.readFileSync(sqlFile, 'utf8');

    // Extract the params block using regex
    const paramsMatch = sqlTemplate.match(/params={([^}]*)}/);
    if (!paramsMatch) {
        console.error('No params found in the file.');
        return;
    }

    // Remove the params block, including the closing '}' from the SQL template
    sqlTemplate = sqlTemplate.replace(/, params={[^}]*}}/, '');
	sqlTemplate = sqlTemplate.trimEnd();
    sqlTemplate = sqlTemplate.replace(/'$/,'');

    // Split the params string into key-value pairs
    const paramsString = paramsMatch[1];
    const paramsArray = paramsString.split(',');

    // Create an object to store key-value pairs
    const paramsMap = {};
    paramsArray.forEach(param => {
        const [key, value] = param.split('=').map(s => s.trim());
        paramsMap[key] = value;
    });

    // Replace placeholders in the SQL template
    Object.keys(paramsMap).forEach(key => {
        // Use regex to ensure we replace `:key` exactly
        const regex = new RegExp(`:${key}\\b`, 'g');
        sqlTemplate = sqlTemplate.replace(regex, `'${paramsMap[key]}'`);
    });

    // Output the final static SQL query
    console.log(sqlTemplate);
}

// Example usage: Pass the SQL file as a command-line argument
if (process.argv.length !== 3) {
    console.error('Usage: node translate_sql.js <sql_template_file>');
    process.exit(1);
}

const sqlFile = process.argv[2];
translateSQL(sqlFile);

