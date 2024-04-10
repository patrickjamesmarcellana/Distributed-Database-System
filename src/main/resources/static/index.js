//let currentOperation;
//
//function clearFields() {
//    $("#id").val('')
//    $("#status").val('')
//    $("#timequeued").val('')
//    $("#queuedate").val('')
//    $("#starttime").val('')
//    $("#endtime").val('')
//    $("#appttype").val('')
//    $("#isvirtual").val('')
//    $("#px_age").val('')
//    $("#px_gender").val('')
//    $("#clinic_hospitalname").val('')
//    $("#clinic_ishospital").val('')
//    $("#clinic_city").val('')
//    $("#clinic_province").val('')
//    $("#clinic_regionname").val('')
//    $("#doctor_mainspecialty").val('')
//    $("#doctor_age").val('')
//}
//
//
//// change displayed input fields depending on selected database operation
//$("#operation").change(() => {
//    // determine selected database operation
//    currentOperation = $("#operation option:selected").attr("value");
//    console.log("Selected Database Operation: " + currentOperation);
//
//    // reset default values of input fields
//    clearFields()
//
//    // show only the ID input when operation is get or delete
//    if (currentOperation === "get" || currentOperation === "delete") {
//        $(".getAll-op").removeClass("hidden");
//        $(".insert-delete-op").addClass("hidden");
//    }
//
//    // show no input field when operation is getAll
//    if (currentOperation === "getAll") {
//        $(".getAll-op").addClass("hidden");
//    }
//
//    // show all input fields when operation is insert or update
//    if (currentOperation === "add" || currentOperation === "update") {
//        $(".getAll-op").removeClass("hidden");
//        $(".insert-delete-op").removeClass("hidden");
//    }
//
//    if (currentOperation === "update") {
//        $(".update-text").removeClass("hidden")
//    } else {
//        $(".update-text").addClass("hidden")
//    }
//
//    if (currentOperation === "add") {
//        $(".insert-op").addClass("hidden")
//    } else {
//        $(".insert-op").removeClass("hidden")
//    }
//
//    $("#data-display").addClass("hidden");
//    $("#error-display").addClass("hidden");
//});
//
//$("#id").change(async () => {
//    // determine selected database operation
//    const currentOperation = $("#operation option:selected").attr("value");
//
//    if (currentOperation !== "update") {
//        return
//    }
//
//    const update_id = $("#id").val()
//    try {
//        const response = await fetch("/appointments/" + update_id)
//        const data = await response.json()
//        console.log(data)
//
//        // change input default values
//        $("#status").val(data.status)
//        $("#timequeued").val(data.timequeued)
//        $("#queuedate").val(data.queuedate)
//        $("#starttime").val(data.starttime)
//        $("#endtime").val(data.endtime)
//        $("#appttype").val(data.appttype)
//        $("#isvirtual").val(data.isvirtual)
//        $("#px_age").val(data.px_age)
//        $("#px_gender").val(data.px_gender)
//        $("#clinic_hospitalname").val(data.clinic_hospitalname)
//        $("#clinic_ishospital").val(data.clinic_ishospital)
//        $("#clinic_city").val(data.clinic_city)
//        $("#clinic_province").val(data.clinic_province)
//        $("#clinic_regionname").val(data.clinic_regionname)
//        $("#doctor_mainspecialty").val(data.doctor_mainspecialty)
//        $("#doctor_age").val(data.doctor_age)
//    } catch (err) {
//        console.log ("ID does not exist in the database")
//    }
//})
//
//$("#submit-btn").click(async (e) => {
//    e.preventDefault();
//
//    $("#data-display").addClass("hidden")
//    $("#error-display").addClass("hidden");
//
//    if (currentOperation === "add") {
//        const data = formToObject();
//        data["id"] = undefined;
//
//        // map island according to region
//        switch (data["clinic_regionname"]) {
//            case "Ilocos Region (I)":
//            case "Cagayan Valley (II)":
//            case "Central Luzon (III)":
//            case "CALABARZON (IV-A)":
//            case "MIMAROPA (IV-B)":
//            case "Bicol Region (V)":
//            case "National Capital Region (NCR)":
//            case "Cordillera Administrative Region (CAR)": data["island"] = "Luzon"; break;
//            case "Western Visayas (VI)":
//            case "Central Visayas (VII)":
//            case "Eastern Visayas (VIII)": data["island"] = "Visayas"; break;
//            case "Zamboanga Peninsula (IX)":
//            case "Northern Mindanao (X)":
//            case "Davao Region (XI)":
//            case "SOCCSKSARGEN (Cotabato Region) (XII)":
//            case "Caraga (XIII)":
//            case "Bangsamoro Autonomous Region in Muslim Mindanao (BARMM)": data["island"] = "Mindanao"; break;
//        }
//
//        console.log("add operation, got:", data);
//        const response = await fetch("/appointments/add", {
//            method: "POST",
//            headers: {
//                "Content-Type": "application/json",
//            },
//            body: JSON.stringify(data),
//        })
//            .then((response) => {
//                if (!response.ok) {
//                     throw new Error("Error adding appointment");
//                }
//                return response.text();
//            })
//            .then((data) => {
//                console.log(data);
//
//                const displayMessage = $("<div>")
//                    .addClass("p-4 text-sm text-green-800 rounded-lg bg-gray-300 dark:bg-green-400 dark:text-green-950")
//                    .attr("role", "alert")
//                    .html("<span class='font-medium'>Success:</span> Appointment has successfully been added.");
//
//                $("#error-display").html(displayMessage);
//                $("#error-display").removeClass("hidden");
//            })
//            .catch((error) => {
//                console.error(error);
//
//                const errorMessage = $("<div>")
//                    .addClass("p-4 text-sm text-red-800 rounded-lg bg-gray-400 dark:bg-red-400 dark:text-red-950")
//                    .attr("role", "alert")
//                    .html("<span class='font-medium'>Error:</span> An error occurred while adding the appointment. Please try again.");
//
//                $("#error-display").html(errorMessage);
//                $("#error-display").removeClass("hidden");
//            });
//        console.log(response);
//        clearFields();
//    }
//
//    if (currentOperation === "delete") {
//        const formData = formToObject();
//        const apptId = formData["id"];
//        console.log("delete operation, got:", apptId);
//        const response = await fetch(`/appointments/delete/${apptId}`, {
//            method: "DELETE",
//        })
//            .then((response) => {
//                if (!response.ok) {
//                     throw new Error("Error deleting appointment");
//                }
//                return response.text();
//            })
//            .then((data) => {
//
//                console.log(data);
//
//                const displayMessage = $("<div>")
//                    .addClass("p-4 text-sm text-green-800 rounded-lg bg-gray-300 dark:bg-green-400 dark:text-green-950")
//                    .attr("role", "alert")
//                    .html("<span class='font-medium'>Success:</span> Appointment has successfully been deleted.");
//
//                $("#error-display").html(displayMessage);
//                $("#error-display").removeClass("hidden");
//            })
//            .catch((error) => {
//
//                console.error(error);
//
//                const errorMessage = $("<div>")
//                    .addClass("p-4 text-sm text-red-800 rounded-lg bg-gray-400 dark:bg-red-400 dark:text-red-950")
//                    .attr("role", "alert")
//                    .html("<span class='font-medium'>Error:</span> An error occurred while deleting the appointment. Please try again.");
//
//                $("#error-display").html(errorMessage);
//                $("#error-display").removeClass("hidden");
//            });
//        console.log(response);
//    }
//
//    if (currentOperation === "update") {
//        const data = formToObject();
//
//        // map island according to region
//        switch (data["clinic_regionname"]) {
//            case "Ilocos Region (I)":
//            case "Cagayan Valley (II)":
//            case "Central Luzon (III)":
//            case "CALABARZON (IV-A)":
//            case "MIMAROPA (IV-B)":
//            case "Bicol Region (V)":
//            case "National Capital Region (NCR)":
//            case "Cordillera Administrative Region (CAR)": data["island"] = "Luzon"; break;
//            case "Western Visayas (VI)":
//            case "Central Visayas (VII)":
//            case "Eastern Visayas (VIII)": data["island"] = "Visayas"; break;
//            case "Zamboanga Peninsula (IX)":
//            case "Northern Mindanao (X)":
//            case "Davao Region (XI)":
//            case "SOCCSKSARGEN (Cotabato Region) (XII)":
//            case "Caraga (XIII)":
//            case "Bangsamoro Autonomous Region in Muslim Mindanao (BARMM)": data["island"] = "Mindanao"; break;
//        }
//
//        console.log("update operation, got:", data);
//        const response = await fetch(`/appointments/update/${data["id"]}`, {
//            method: "PUT",
//            headers: {
//                "Content-Type": "application/json",
//            },
//            body: JSON.stringify(data),
//        })
//            .then((response) => {
//                console.log(response)
//                if (!response.ok) {
//                     throw new Error("Error updating appointment");
//                }
//                return response.text();
//            })
//            .then((data) => {
//
//                console.log(data);
//
//                const displayMessage = $("<div>")
//                    .addClass("p-4 text-sm text-green-800 rounded-lg bg-gray-300 dark:bg-green-400 dark:text-green-950")
//                    .attr("role", "alert")
//                    .html("<span class='font-medium'>Success:</span> Appointment has successfully been updated.");
//
//                $("#error-display").html(displayMessage);
//                $("#error-display").removeClass("hidden");
//            })
//            .catch((error) => {
//
//                console.error(error);
//
//                const errorMessage = $("<div>")
//                    .addClass("p-4 text-sm text-red-800 rounded-lg bg-gray-400 dark:bg-red-400 dark:text-red-950")
//                    .attr("role", "alert")
//                    .html("<span class='font-medium'>Error:</span> An error occurred while updating the appointment. Please try again.");
//
//                $("#error-display").html(errorMessage);
//                $("#error-display").removeClass("hidden");
//            });
//        console.log(response);
//    }
//
//    if (currentOperation === "getAll") {
//      console.log("getAll operation");
//      const response = await fetch("/appointments/getAll", {
//          method: "GET",
//          })
//          .then(response => {
//              if (!response.ok) {
//                  throw new Error("Error fetching data");
//              }
//              return response.json();
//          })
//          .then(appointments => {
//              console.log("Appointments received:", appointments);
//
//              const tableContainer = $("<div>").addClass("px-4");
//              const table = $("<table>").addClass("text-sm text-left rtl:text-right mt-1 overflow-x-auto overflow-y-auto h-screen");
//              tableContainer.append(table);
//
//              const thead = $("<thead>").addClass("text-xs text-black uppercase rounded border-b dark:text-black");
//              const headerRow = $("<tr>");
//
//              const headerLabels = ["Appointment ID", "Status", "Time Queued", "Queue Date", "Start Time", "End Time", "Appointment Type", "Virtual", "Patient Age", "Patient Gender", "Clinic/Hospital Name", "Clinic is Hospital", "Clinic City", "Clinic Province", "Clinic Region Name", "Doctor Main Specialty", "Doctor Age"];
//              $.each(headerLabels, function(index, label) {
//                  $("<th>").text(label).addClass("px-6 py-3").appendTo(headerRow);
//              });
//
//              headerRow.appendTo(thead);
//              thead.appendTo(table);
//
//              const tbody = $("<tbody>").attr("id", "appointments-list");
//              $.each(appointments, function(index, appointment) {
//                  const row = $("<tr>").addClass("border-b hover:bg-gray-300");
//
//                  const appointmentAttributes = ["id", "status", "timequeued", "queuedate", "starttime", "endtime", "appttype", "isvirtual", "px_age", "px_gender", "clinic_hospitalname", "clinic_ishospital", "clinic_city", "clinic_province", "clinic_regionname", "doctor_mainspecialty", "doctor_age"];
//
//                  $.each(appointmentAttributes, function(index, attr) {
//                      $("<td>").text(appointment[attr]).addClass("px-6 py-4").appendTo(row);
//                  });
//
//                  tbody.append(row);
//              });
//
//              table.append(tbody);
//
//              $("#data-display").empty().append(table);
//              $("#data-display").removeClass("hidden");
//          })
//           .catch((error) => {
//
//              console.error(error);
//
//              const errorMessage = $("<div>")
//                  .addClass("p-4 text-sm text-red-800 rounded-lg bg-gray-400 dark:bg-red-400 dark:text-red-950")
//                  .attr("role", "alert")
//                  .html("<span class='font-medium'>Error:</span> An error occurred while finding all appointments. Please try again.");
//
//              $("#error-display").html(errorMessage);
//              $("#error-display").removeClass("hidden");
//          });
//
//    }
//
//    if (currentOperation === "get") {
//          console.log("get operation");
//          const data = formToObject();
//          const response = await fetch(`/appointments/${data.id}`, {
//              method: "GET",
//              })
//              .then(response => {
//                  if (!response.ok) {
//                      throw new Error("Error fetching data");
//                  }
//                  return response.json();
//              })
//              .then(appointment => {
//                  console.log("Appointment received:", appointment);
//
//                  const tableContainer = $("<div>").addClass("px-4");
//                  const table = $("<table>").addClass("text-sm text-left rtl:text-right mt-1 overflow-x-auto overflow-y-auto h-0.5");
//                  tableContainer.append(table);
//
//                  const thead = $("<thead>").addClass("text-xs text-black uppercase rounded border-b dark:text-black");
//                  const headerRow = $("<tr>");
//
//                  const headerLabels = ["Appointment ID", "Status", "Time Queued", "Queue Date", "Start Time", "End Time", "Appointment Type", "Virtual", "Patient Age", "Patient Gender", "Clinic/Hospital Name", "Clinic is Hospital", "Clinic City", "Clinic Province", "Clinic Region Name", "Doctor Main Specialty", "Doctor Age"];
//
//                  $.each(headerLabels, function(index, label) {
//                      $("<th>").text(label).addClass("px-6 py-3").appendTo(headerRow);
//                  });
//
//                  headerRow.appendTo(thead);
//                  thead.appendTo(table);
//
//                  const tbody = $("<tbody>").attr("id", "appointments-list");
//
//                  const row = $("<tr>").addClass("border-b hover:bg-gray-300");
//
//                  const appointmentAttributes = ["id", "status", "timequeued", "queuedate", "starttime", "endtime", "appttype", "isvirtual", "px_age", "px_gender", "clinic_hospitalname", "clinic_ishospital", "clinic_city", "clinic_province", "clinic_regionname", "doctor_mainspecialty", "doctor_age"];
//
//                  $.each(appointmentAttributes, function(index, attr) {
//                      $("<td>").text(appointment[attr]).addClass("px-6 py-4").appendTo(row);
//                  });
//                  tbody.append(row);
//
//                  table.append(tbody);
//
//                  $("#data-display").empty().append(table);
//                  $("#data-display").removeClass("hidden");
//              })
//              .catch((error) => {
//
//                console.error(error);
//
//                const errorMessage = $("<div>")
//                    .addClass("p-4 text-sm text-red-800 rounded-lg bg-gray-400 dark:bg-red-400 dark:text-red-950")
//                    .attr("role", "alert")
//                    .html("<span class='font-medium'>Error:</span> An error occurred while finding this appointment. Please try again.");
//
//                $("#error-display").html(errorMessage);
//                $("#error-display").removeClass("hidden");
//              });
//
//    }
//});
//
//function formToObject() {
//    return {
//        id: $("#id").val(),
//        status: $("#status").val(),
//        timequeued: $("#timequeued").val(),
//        queuedate: $("#queuedate").val(),
//        starttime: $("#starttime").val(),
//        endtime: $("#endtime").val(),
//        appttype: $("#appttype").val(),
//        isvirtual: $("#isvirtual").val(),
//        px_age: $("#px_age").val(),
//        px_gender: $("#px_gender").val(),
//        clinic_hospitalname: $("#clinic_hospitalname").val(),
//        clinic_ishospital: $("#clinic_ishospital").val(),
//        clinic_city: $("#clinic_city").val(),
//        clinic_province: $("#clinic_province").val(),
//        clinic_regionname: $("#clinic_regionname").val(),
//        doctor_mainspecialty: $("#doctor_mainspecialty").val(),
//        doctor_age: $("#doctor_age").val(),
//    };
//}

$("#submit-btn").click(async (e) => {
    e.preventDefault()
    const data = {
        node: $("#node").val(),
        isolationLevel: $("#isolation-level").val(),
        transaction: $("#transaction").val(),
        operation: $("#operation-type").val(),
        id: $("#id").val(),
        sleepOrNot: $("#sleep-or-not").val(),
        commitOrRollback: $("#commit-or-rollback").val()
    }

    if (data.operation === "Read") {
        try {
            const result = await fetch(`/appointments/read?node=${data.node}&isolationLevel=${data.isolationLevel}&transaction=${data.transaction}&operation=${data.operation}&id=${data.id}&sleepOrNot=${data.sleepOrNot}&commitOrRollback=${data.commitOrRollback}`, {method: "GET"})
            const appointment = await result.json()
            console.log(appointment)
            const appointments = [appointment]
            displayResult(appointments)
            displaySuccess("read")
        } catch (err) {
            console.error(err)
            displayError("reading this appointment.")
        }
    } else if (data.operation === "Update") {
        // write (update) operation

        try {
            const result = await fetch(`/appointments/update`, {
                method: "PATCH",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(data),
            })
            const appointment = await result.json()
            console.log(appointment)
            displayResult(appointment)
            displaySuccess("updated")
        } catch (err) {
            console.error(err)
            displayError("updating this appointment.")
        }
    } else if (data.operation === "Delete") {
        // write (delete) operation

        try {
            const result = await fetch(`/appointments/update`, {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json",
                },
                body: JSON.stringify(data),
            })
            console.log(result)
            if (result.status === 200) {
                displaySuccess("deleted")
            }
        } catch (err) {
            console.error(err)
            displayError("deleting this appointment.")
        }
    } else if(data.operation === "Find All") {
        // find all operation
        try {
            const response = await fetch(`/appointments/findAll?node=${data.node}&transaction=${data.transaction}&operation=${data.operation}`, {method: "GET"})
            if (!response.ok) {
                throw new Error("Error fetching data")
            }
            const result = await response.json()
            console.log(result)
            displayResult(result)
            displaySuccess("found")
        } catch (err) {
            console.error(err)
            displayError("finding all appointments")
        }
    } else {
        // should be impossible
        console.log("Incorrect mapping of database operation in index.js")
    }

    clearFields()
})

function clearFields() {
    $("#node").val("")
    $("#isolation-level").val("")
    $("#transaction").val("")
    $("#operation-type").val(""),
    $("#id").val(""),
    $("#sleep-or-not").val("")
    $("#commit-or-rollback").val("")
}

function displayResult(appointments) {
     const tableContainer = $("<div>").addClass("px-4");
     const table = $("<table>").addClass("text-sm text-left rtl:text-right mt-1");
     tableContainer.append(table);

     const thead = $("<thead>").addClass("text-xs text-black uppercase rounded border-b dark:text-black");
     const headerRow = $("<tr>");

     const headerLabels = ["Appointment ID", "Status", "Time Queued", "Queue Date", "Start Time", "End Time", "Appointment Type", "Virtual", "Patient Age", "Patient Gender", "Clinic/Hospital Name", "Clinic is Hospital", "Clinic City", "Clinic Province", "Clinic Region Name", "Island", "Doctor Main Specialty", "Doctor Age"];
     $.each(headerLabels, function(index, label) {
         $("<th>").text(label).addClass("px-6 py-3").appendTo(headerRow);
     });

     headerRow.appendTo(thead);
     thead.appendTo(table);

     const tbody = $("<tbody>").attr("id", "appointments-list");
     $.each(appointments, function(index, appointment) {
         const row = $("<tr>").addClass("border-b hover:bg-gray-300");

         const appointmentAttributes = ["id", "status", "timequeued", "queuedate", "starttime", "endtime", "appttype", "isvirtual", "px_age", "px_gender", "clinic_hospitalname", "clinic_ishospital", "clinic_city", "clinic_province", "clinic_regionname", "island", "doctor_mainspecialty", "doctor_age"];

         $.each(appointmentAttributes, function(index, attr) {
             $("<td>").text(appointment[attr]).addClass("px-6 py-4").appendTo(row);
         });

         tbody.append(row);
     });

     table.append(tbody);

     $("#data-display").empty().append(table);
     $("#data-display").removeClass("hidden");
}

function displayError(operation) {
    const errorMessage = $("<div>")
        .addClass("p-4 text-sm text-red-800 rounded-lg bg-gray-400 dark:bg-red-400 dark:text-red-950 mb-6")
        .attr("role", "alert")
        .html("<span class='font-medium'>Error:</span> An error occurred while " + operation + ". Please try again.");

    $("#error-display").html(errorMessage);
    $("#error-display").removeClass("hidden");
}

function displaySuccess(operation) {
    const displayMessage = $("<div>")
        .addClass("p-4 text-sm text-green-800 rounded-lg bg-gray-300 dark:bg-green-400 dark:text-green-950 mb-6")
        .attr("role", "alert")
        .html("<span class='font-medium'>Success:</span> Appointment has been successfully " + operation +".");

    $("#error-display").html(displayMessage);
    $("#error-display").removeClass("hidden");
}