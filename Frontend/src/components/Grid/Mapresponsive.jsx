/* global window */
import React, { Component } from "react";
import MapGL, { Marker } from "react-map-gl";
import { FaCircle } from "react-icons/fa";

export default class Mapresponsive extends Component {
  constructor(props) {
    super(props);
    this.state = {
      style: "mapbox://styles/mapbox/light-v9",
      viewport: {
        width: window.innerWidth,
        height: window.innerHeight,
        longitude: 9.93228,
        latitude: 51.53443,
        zoom: 5,
      },
    };
  }

  componentDidMount() {
    window.addEventListener("resize", this._resize);
    this._resize();
  }

  componentWillUnmount() {
    window.removeEventListener("resize", this._resize);
  }

  onStyleChange = (style) => {
    this.setState({ style });
  };

  _onViewportChange = (viewport) => {
    this.setState({
      viewport: { ...this.state.viewport, ...viewport },
    });
  };

  _resize = () => {
    this._onViewportChange({
      width: window.innerWidth / 2 - 45,
      height: window.innerHeight - 190,
    });
  };

  selectedStyle = (truck_id) =>
    this.props.selectedTruckId === truck_id
      ? {
          color: "#141e56",
          fontSize: "20px",
        }
      : { color: "#3f51b5" };

  render() {
    return (
      <div style={{ borderRadius: 20 + "px" }}>
        <MapGL
          {...this.state.viewport}
          mapboxApiAccessToken="pk.eyJ1Ijoic2gyNjciLCJhIjoiY2tiZnIxd3QyMHkzMjJ2cWUyYWxtbzE0eCJ9.xqX_RuC9dWfDaRogdcZmEQ"
          mapStyle="mapbox://styles/sh267/ckbj5k4xo24no1iqnqq2xso7r"
          onViewportChange={(viewport) => this._onViewportChange(viewport)}
        >
          {this.props.datasample.map((datapoint) => (
            <Marker
              key={datapoint.truck_id}
              latitude={datapoint.telemetry_lat}
              longitude={datapoint.telemetry_lon}
              className={
                this.props.selectedTruckId === datapoint.truck_id
                  ? "truck-marker truck-marker-selected"
                  : "truck-marker"
              }
            >
              {/* <AdjustIcon
                className="truck-marker-glow"
                style={this.selectedStyle(datapoint.truck_id)}
                onClick={() =>
                  this.props.setSelectedTruckId(datapoint.truck_id)
                }
              ></AdjustIcon> */}
              <FaCircle
                className="truck-marker-glow"
                style={this.selectedStyle(datapoint.truck_id)}
                onClick={() =>
                  this.props.setSelectedTruckId(datapoint.truck_id)
                }
              />
            </Marker>
          ))}
        </MapGL>
      </div>
    );
  }
}
