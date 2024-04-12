const _read = async(data) => { 
    const result = await fetch((`http://localhost:8080/appointments/read?node=${data.node}&isolationLevel=${data.isolationLevel}&transaction=${data.transaction}&operation=${data.operation}&id=${data.id}&sleepOrNot=${data.sleepOrNot}&commitOrRollback=${data.commitOrRollback}`), {method: "GET"})
    if(result.status == 200)
        return await result.json();
};

const _update = async(data) => { 
    const result = fetch(`http://localhost:8080/appointments/update`, {
        method: "PATCH",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
    }) 

    if(result.status == 200)
        return await result.json();
};

const _delete = async(data) => { 
    await fetch(`http://localhost:8080/appointments/delete`, {
        method: "DELETE",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify(data),
    }) 
};

const test_1_1 = async () => {
    const data2 = {
        node: "20189",
        isolationLevel: "READ UNCOMMITTED",
        transaction: "SELECT * FROM appointments WHERE id = ?;",
        operation: "Read",
        id: "1",
        sleepOrNot: "sleep-before",
        commitOrRollback: "commit"
    }
    const data3 = {
        node: "20190",
        isolationLevel: "READ UNCOMMITTED",
        transaction: "SELECT * FROM appointments WHERE id = ?;",
        operation: "Read",
        id: "1",
        sleepOrNot: "dont-sleep",
        commitOrRollback: "commit"
    }
    
    const ret = await Promise.all([_read(data2), _read(data3)]);
    console.log(ret)
}


const test_3_2 = async () => {
    const data2 = {
        node: "20189",
        isolationLevel: "SERIALIZABLE",
        transaction: "UPDATE appointments SET px_age = px_age + 5 WHERE id = ?;",
        operation: "Update",
        id: "1000",
        sleepOrNot: "sleep-before",
        commitOrRollback: "commit"
    }
    const data3 = {
        node: "20189",
        isolationLevel: "SERIALIZABLE",
        transaction: "DELETE FROM appointments WHERE id = ?;",
        operation: "Delete",
        id: "1000",
        sleepOrNot: "dont-sleep",
        commitOrRollback: "commit"
    }
    
    const ret = await Promise.all([_update(data2), _delete(data3)]);
    console.log(ret)
}

const test_3_3 = async () => {
    const data2 = {
        node: "20189",
        isolationLevel: "SERIALIZABLE",
        transaction: "DELETE FROM appointments WHERE id = ?;",
        operation: "Delete",
        id: "890",
        sleepOrNot: "sleep-before",
        commitOrRollback: "commit"
    }
    const data3 = {
        node: "20189",
        isolationLevel: "SERIALIZABLE",
        transaction: "DELETE FROM appointments WHERE id = ?;",
        operation: "Delete",
        id: "890",
        sleepOrNot: "dont-sleep",
        commitOrRollback: "commit"
    }
    
    const ret = await Promise.all([_delete(data2), _delete(data3)]);
    console.log(ret)
}

test_1_1();