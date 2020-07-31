import React, { Component } from "react";
import { FaChartBar, FaMapMarked, FaHistory } from "react-icons/fa";

class Sidebar extends Component {
  state = {};
  render() {
    return (
      <div className="Sidebar">
        <div className="Logo">horizon</div>
        <div className="Item Active">
          <FaMapMarked />
          <div className="Item-Text">live</div>
        </div>
        <div className="Item">
          <FaChartBar />
          <div className="Item-Text">detail</div>
        </div>
        <div className="Item">
          <FaHistory />
          <div className="Item-Text">history</div>
        </div>
      </div>
    );
  }
}

export default Sidebar;
