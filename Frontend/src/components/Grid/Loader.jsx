import React, { useEffect } from "react";
import { makeStyles } from "@material-ui/core/styles";
import Fade from "@material-ui/core/Fade";
import Logo from "../../logo_style_transparent.png";

const useStyles = makeStyles((theme) => ({
  root: {
    zIndex: "20",
    position: "absolute",
    margin: "0",
    padding: "0",
  },
  container: {
    display: "flex",
    position: "absolute",
    background:
      "linear-gradient(90deg, rgba(2,0,36,1) 0%, rgba(9,9,121,1) 35%, rgba(0,212,255,1) 100%)",
    margin: "0",
    padding: "0",
  },
  paper: {
    width: "100vw",
    height: "100vh",
    margin: "0",
    padding: "0",
    display: "flex",
    alignItems: "center",
  },
}));

export default function SimpleCollapse(props) {
  const classes = useStyles();
  const [logoBackground, setlogoBackground] = React.useState(true);
  const [logo, setLogo] = React.useState(true);
  const firstLoad = props.isLoading;
  const [loaded, setLoaded] = React.useState(false);

  useEffect(() => {
    if (firstLoad) {
      setInterval(() => {
        setlogoBackground(false);
      }, 2800);
      setInterval(() => {
        setLogo(false);
      }, 2300);
    } else {
      console.log("Loading...");
    }
  });

  return (
    <div className={classes.root}>
      <Fade in={logoBackground}>
        <div className={classes.container}>
          <Fade in={logo}>
            {/* <Paper elevation={4} className={classes.paper}> */}
            <div className={classes.paper}>
              <img
                style={
                  loaded
                    ? {
                        marginLeft: "auto",
                        marginRight: "auto",
                        verticalAlign: "middle !important",
                        display: "block",
                        width: "20%",
                      }
                    : { display: "none" }
                }
                onLoad={() => setLoaded({ loaded: true })}
                src={Logo}
                alt="Logo"
              />
            </div>
            {/* </Paper> */}
          </Fade>
        </div>
      </Fade>
    </div>
  );
}
