import React, { useRef, useState, useCallback, useEffect } from 'react';
import Modal from './Modal';
import Button from './Button';
import { Camera, CheckCircle2, ShieldAlert } from 'lucide-react';
import api from '../../services/api';
import { useToast } from '../../context/ToastContext';

export default function FaceConfigModal({ isOpen, onClose }) {
  const { success: toastSuccess, error: toastError } = useToast();
  const videoRef = useRef(null);
  const canvasRef = useRef(null);
  const [stream, setStream] = useState(null);
  const [capturedImage, setCapturedImage] = useState(null);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  const stopCamera = useCallback(() => {
    if (stream) {
      stream.getTracks().forEach(track => track.stop());
      setStream(null);
    }
  }, [stream]);

  const startCamera = async () => {
    setError('');
    setCapturedImage(null);
    try {
      const mediaStream = await navigator.mediaDevices.getUserMedia({ video: true });
      setStream(mediaStream);
      if (videoRef.current) {
        videoRef.current.srcObject = mediaStream;
      }
    } catch (err) {
      setError('Camera permission denied or not available.');
    }
  };

  useEffect(() => {
    if (isOpen) {
      startCamera();
    } else {
      stopCamera();
    }
    return () => stopCamera();
  }, [isOpen]);

  const captureFace = () => {
    if (videoRef.current && canvasRef.current) {
      const context = canvasRef.current.getContext('2d');
      canvasRef.current.width = videoRef.current.videoWidth;
      canvasRef.current.height = videoRef.current.videoHeight;
      context.drawImage(videoRef.current, 0, 0, canvasRef.current.width, canvasRef.current.height);
      const dataUrl = canvasRef.current.toDataURL('image/jpeg');
      setCapturedImage(dataUrl);
      stopCamera();
    }
  };

  const submitFace = async () => {
    if (!capturedImage) return;
    setSubmitting(true);
    try {
      await api.post('/face-verification/configure', {
        faceImageBase64: capturedImage
      });
      toastSuccess('Face image configured securely.');
      onClose();
    } catch (err) {
      toastError('Failed to configure face image.');
      setError('Failed to save face image.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <Modal
      isOpen={isOpen}
      onClose={onClose}
      title="Configure Face Verification"
      subtitle="Register your face to secure high-risk transactions."
      footer={
        <div className="flex gap-2">
          <Button variant="secondary" size="sm" onClick={onClose} disabled={submitting}>
            Cancel
          </Button>
          {!capturedImage ? (
            <Button variant="primary" size="sm" onClick={captureFace} icon={Camera} disabled={!stream}>
              Capture Face
            </Button>
          ) : (
            <>
              <Button variant="secondary" size="sm" onClick={startCamera} disabled={submitting}>
                Retake
              </Button>
              <Button variant="primary" size="sm" onClick={submitFace} loading={submitting} icon={CheckCircle2}>
                Save Reference Face
              </Button>
            </>
          )}
        </div>
      }
    >
      <div className="space-y-4 text-center flex flex-col items-center">
        {error && (
          <div className="text-red-400 text-sm flex items-center justify-center gap-1 bg-red-950/20 p-2 rounded w-full">
            <ShieldAlert className="w-4 h-4" />
            {error}
          </div>
        )}
        
        <div className="relative w-64 h-64 bg-navy-900 border border-navy-700/80 rounded-xl overflow-hidden flex items-center justify-center">
          {!capturedImage ? (
            <>
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                className="w-full h-full object-cover"
              />
              <canvas ref={canvasRef} className="hidden" />
              {!stream && !error && <span className="text-slate-400 text-sm">Starting camera...</span>}
            </>
          ) : (
            <img src={capturedImage} alt="Captured face" className="w-full h-full object-cover" />
          )}
        </div>
        <p className="text-xs text-slate-400 max-w-sm">
          Please make sure your face is clearly visible, well-lit, and directly facing the camera.
        </p>
      </div>
    </Modal>
  );
}
