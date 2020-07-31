import React, { Component, useState } from "react";
import { FaLeaf, FaExclamationTriangle, FaCircle } from "react-icons/fa";

function Detailrow(props) {
  const driver_acceleration =
    parseFloat(props.datapoint.driver_acceleration) > 0 ? 1 : 0;
  const driver_speed = parseFloat(props.datapoint.driver_speed) > 0 ? 1 : 0;
  const driver_brake = parseFloat(props.datapoint.driver_brake) > 0 ? 1 : 0;
  const incident = parseFloat(props.datapoint.incident) === true ? 1 : 0;
  const truck_speed = (props.datapoint.truck_speed * 3.6).toFixed(1);

  const info_amount = driver_acceleration + driver_speed + driver_brake;
  const warn_amount = incident; // to be defined
  const delay_amount = parseFloat(props.datapoint.delay);

  let infostyle = () =>
    info_amount > 0
      ? { color: "#141e56", fontWeight: "bold" }
      : { color: "#EFF4F9" };

  let ecostyle = () =>
    warn_amount > 0
      ? { color: "#141e56", fontWeight: "bold" }
      : { color: "#EFF4F9" };

  let drivingstyle = () =>
    truck_speed > 0
      ? { color: "#5BB5CE", backgroundColor: "#DDF7FB" }
      : { color: "#DD3838", backgroundColor: "#FBDDDD" };

  let delaystyle = () =>
    delay_amount < 0 ? { color: "#5BB5CE" } : { color: "#DD3838" };

  let selectedStyle = () =>
    props.selectedTruckId === props.datapoint.truck_id
      ? { boxShadow: "0 4px 8px 0 rgba(0, 0, 0, 0.15)" }
      : { boxShadow: "none" };
  // console.log(props.selectedTruckId, props.datapoint.data_id);
  return (
    <div
      className="flex-container"
      onClick={() => props.setSelectedTruckId(props.datapoint.truck_id)}
      style={selectedStyle()}
    >
      <div className="left-column">
        <div className="truck-number-plate">{props.datapoint.number_plate}</div>
        <div className="truck-info">
          <div className="truck-info-warning" style={infostyle()}>
            <FaExclamationTriangle
              style={{ width: "15px", paddingRight: "5px" }}
            />
            {String(info_amount)}
          </div>
          <div className="truck-info-eco" style={ecostyle()}>
            <FaLeaf style={{ width: "15px", paddingRight: "5px" }} />
            {String(warn_amount)}
          </div>
        </div>
      </div>
      <div className="right-column">
        <div className="truck-route">
          {props.datapoint.departure} - {props.datapoint.arrival}
        </div>
        <div className="truck-delay" style={delaystyle()}>
          {props.datapoint.delay} min
        </div>
        <div className="truck-speed">
          <span style={drivingstyle()}>
            <FaCircle style={{ width: "7px", paddingRight: "5px" }} />
            {String(truck_speed)} km/h
          </span>
        </div>
      </div>
    </div>
  );
}

export default Detailrow;
