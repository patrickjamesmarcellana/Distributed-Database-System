let currentOperation;

// change displayed input fields depending on selected database operation
$("#operation").change(() => {
    // determine selected database operation
    currentOperation = $("#operation option:selected").attr("value");
    console.log("Selected Database Operation: " + currentOperation);

    // show only the ID input when operation is get or delete
    if (currentOperation === "get" || currentOperation === "delete") {
        $(".getAll-op").removeClass("hidden");
        $(".insert-delete-op").addClass("hidden");
    }

    // show no input field when operation is getAll
    if (currentOperation === "getAll") {
        $(".getAll-op").addClass("hidden");
    }

    // show all input fields when operation is insert or update
    if (currentOperation === "add" || currentOperation === "update") {
        $(".getAll-op").removeClass("hidden");
        $(".insert-delete-op").removeClass("hidden");
    }
});

$("#submit-btn").click(async (e) => {
    e.preventDefault();

    if (currentOperation === "add") {
        const data = formToObject();
        console.log("add operation, got:", data);
        const response = await fetch("/appointments/add", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(data),
        })
            .then((response) => {
                return response.text();
            })
            .catch((error) => console.error(error));
        console.log(response);
    }
});

function formToObject() {
    return {
        id: $("#id").val(),
        status: $("#status").val(),
        timequeued: $("#timequeued").val(),
        queuedate: $("#queuedate").val(),
        starttime: $("#starttime").val(),
        endtime: $("#endtime").val(),
        appttype: $("#appttype").val(),
        isvirtual: $("#isvirtual").val(),
        px_age: $("#px_age").val(),
        px_gender: $("#px_gender").val(),
        clinic_hospitalname: $("#clinic_hospitalname").val(),
        clinic_ishospital: $("#clinic_ishospital").val(),
        clinic_city: $("#clinic_city").val(),
        clinic_province: $("#clinic_province").val(),
        clinic_regionname: $("#clinic_regionname").val(),
        doctor_mainspecialty: $("#doctor_mainspecialty").val(),
        doctor_age: $("#doctor_age").val(),
    };
}
